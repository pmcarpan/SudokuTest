package io.github.pmcarpan.sudoku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

// Linear version of SudokuExtractor.java
public class SudokuExtract {

    public static void main(String[] args) {
        // Load library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // read image, grayscale
        Mat sudoku = Imgcodecs.imread("images/sudokubig.jpg", 0);

        // closing, normalizing
        Mat closing = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(11, 11));
        Imgproc.morphologyEx(sudoku, closing, Imgproc.MORPH_CLOSE, kernel);

        Mat result = new Mat();
        Core.divide(sudoku, closing, result, 1.0, CvType.CV_64F);

        Core.normalize(result, result, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);

        
        
        // blur, threshold
        Mat thresh = new Mat(), blur = new Mat();
        Imgproc.GaussianBlur(result, blur, new Size(11, 11), 0);
        Imgproc.adaptiveThreshold(blur, thresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);

        // find largest connected area via flood fill -> outer frame
        int max = -1;
        Point max_point = null;
        Mat outer_frame = thresh.clone();
        Imgcodecs.imwrite("images/sudoku-thresh.png", thresh);

        for (int y = 0; y < outer_frame.rows(); y++) {
            for (int x = 0; x < outer_frame.cols(); x++) {
                if (outer_frame.get(y, x)[0] < 128) {
                    int area = Imgproc.floodFill(outer_frame, new Mat(), 
                            new Point(x, y), new Scalar(130));
                    if (area > max) {
                        max_point = new Point(x, y);
                        max = area;
                    }
                }
            }
        }

        // fill the max area black
        Imgproc.floodFill(outer_frame, new Mat(), max_point, new Scalar(0));

        // white out the remaining flood filled areas
        for (int y = 0; y < outer_frame.size().height; y++) {
            for (int x = 0; x < outer_frame.size().width; x++) {
                if (outer_frame.get(y, x)[0] == 130) {
                    Imgproc.floodFill(outer_frame, new Mat(), new Point(x, y), new Scalar(255));
                }
            }
        }

        // generate a mask for the grid, and get grid
        Mat mask = new Mat();
        Core.bitwise_not(outer_frame, mask);

        Imgproc.floodFill(outer_frame, new Mat(), new Point(0, 0), new Scalar(0));

        Core.bitwise_or(mask, outer_frame, mask);
        // Core.bitwise_and(result, mask, result);

        Imgcodecs.imwrite("images/sudoku-output.png", result);
        
        
        // Mat result_blur = new Mat();
        // Imgproc.GaussianBlur(result, result_blur, new Size(3, 11), 0);

        
        
        
        // 2nd derivative filter for vertical lines
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 13));
        Mat dx = new Mat();
        Imgproc.Sobel(result, dx, CvType.CV_16S, 1, 0);
        Core.convertScaleAbs(dx, dx);
        Core.normalize(dx, dx, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);
        Core.bitwise_and(dx, mask, dx);
        Imgproc.morphologyEx(dx, dx, Imgproc.MORPH_CLOSE, kernel);
        //Imgproc.GaussianBlur(dx, dx, new Size(3, 13), 0);
        //Imgproc.morphologyEx(dx, dx, Imgproc.MORPH_CLOSE, kernel);
        //Imgproc.GaussianBlur(dx, dx, new Size(3, 13), 0);
        Mat thresh_dx = new Mat();
        Imgproc.threshold(dx, thresh_dx, 0, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
        // Imgproc.threshold(dx, thresh_dx, 0.40 * 255, 255, Imgproc.THRESH_BINARY);
        // Imgproc.adaptiveThreshold(dx, thresh_dx, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 45, 0);
        Imgproc.morphologyEx(thresh_dx, thresh_dx, Imgproc.MORPH_OPEN, kernel);

        Imgcodecs.imwrite("images/actual_thresh_dx.png", thresh_dx);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresh_dx, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        contours.sort( (c1, c2) -> -Double.compare( Imgproc.boundingRect(c1).height, Imgproc.boundingRect(c2).height ) ); 
        
        for (int i = 0; i < contours.size(); i++) {
            if (i < 10) // draw largest 10 contours
                Imgproc.drawContours(thresh_dx, contours, i, new Scalar(255), -1);
            else
                Imgproc.drawContours(thresh_dx, contours, i, new Scalar(0), -1);
        }

        Imgproc.dilate(thresh_dx, thresh_dx, kernel);
        


        // 2nd derivative filter for horizontal lines
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13, 3));
        Mat dy = new Mat();
        Imgproc.Sobel(result, dy, -1, 0, 1);
        Core.convertScaleAbs(dy, dy);
        Core.normalize(dy, dy, 0, 255, Core.NORM_MINMAX);
        Core.bitwise_and(dy, mask, dy);
        Imgproc.morphologyEx(dy, dy, Imgproc.MORPH_CLOSE, kernel); 
        // Imgproc.GaussianBlur(dy, dy, new Size(13, 3), 0);
        // Imgproc.morphologyEx(dy, dy, Imgproc.MORPH_DILATE, kernel); 
        // Imgproc.morphologyEx(dy, dy, Imgproc.MORPH_OPEN, kernel); 
        // Imgproc.GaussianBlur(dy, dy, new Size(13, 3), 0);
        // Imgproc.morphologyEx(dy, dy, Imgproc.MORPH_DILATE, kernel); 
        Mat thresh_dy = new Mat();
        Imgproc.threshold(dy, thresh_dy, 0, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
        // Imgproc.adaptiveThreshold(dy, thresh_dy, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 45, 0);
        Imgcodecs.imwrite("images/actual_thresh_dy.png", thresh_dy);
        // Imgproc.morphologyEx(thresh_dy, thresh_dy, Imgproc.MORPH_DILATE, kernel);

        contours = new ArrayList<>();
        Imgproc.findContours(thresh_dy, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        contours.sort( (c1, c2) -> -Double.compare( Imgproc.boundingRect(c1).width, Imgproc.boundingRect(c2).width ) );
        
        for (int i = 0; i < contours.size(); i++) {
            if (i < 10) // draw first 10 contours
                Imgproc.drawContours(thresh_dy, contours, i, new Scalar(255), -1);
            else
                Imgproc.drawContours(thresh_dy, contours, i, new Scalar(0), -1);
        }

        Imgproc.dilate(thresh_dy, thresh_dy, kernel);
        
        
        
        Imgcodecs.imwrite("images/dx.png", dx);
        Imgcodecs.imwrite("images/dy.png", dy);
        Imgcodecs.imwrite("images/thresh-vertical.png", thresh_dx);
        Imgcodecs.imwrite("images/thresh-horizontal.png", thresh_dy);



        // find grid points
        Mat intersections = new Mat();
        Core.bitwise_and(thresh_dx, thresh_dy, intersections);
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(11, 11));
        Imgproc.morphologyEx(intersections, intersections, Imgproc.MORPH_CLOSE, kernel);
        
        Imgcodecs.imwrite("images/intersections.png", intersections);
        
        

        // 
        contours = new ArrayList<>();
        Imgproc.findContours(intersections, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        List<Point> intersection_points = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Moments m = Imgproc.moments(contour);
            double x = m.m10 / m.m00, y = m.m01 / m.m00;
            intersection_points.add(new Point(x, y));
        }

        Point[] intersection_points_array = intersection_points.toArray(new Point[0]);
        System.out.println(intersection_points_array.length);
        Arrays.sort(intersection_points_array, (p1, p2) -> (Double.compare(p1.y, p2.y)));
        for (int i = 0; i < 100; i += 10) {
            Arrays.sort(intersection_points_array, i, i+10, (p1, p2) -> (Double.compare(p1.x, p2.x)));
        }

        //        // display corresponding point and id
        //        int index = 0;
        //        for (Point p : intersection_points_array) {
        //            Imgproc.circle(result, p, 4, new Scalar(255), -1);
        //            Imgproc.putText(result, (index++)+"", p, Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(128));
        //        }

        

        int final_size = 100;
        List<Point> corners_final = new ArrayList<>();
        corners_final.add(new Point(0, 0));
        corners_final.add(new Point(final_size, 0));
        corners_final.add(new Point(0, final_size));
        corners_final.add(new Point(final_size, final_size));
        Mat corners_final_mat = Converters.vector_Point2f_to_Mat(corners_final);

        int[][] sudoku_matrix = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int index = i * 10 + j;
                List<Point> corners_actual = new ArrayList<>();
                corners_actual.add(intersection_points_array[index]);
                corners_actual.add(intersection_points_array[index + 1]);
                corners_actual.add(intersection_points_array[index + 10]);
                corners_actual.add(intersection_points_array[index + 11]);
                Mat corners_actual_mat = Converters.vector_Point2f_to_Mat(corners_actual);

                Mat trans = Imgproc.getPerspectiveTransform(corners_actual_mat, corners_final_mat);

                Mat single_box = new Mat();
                
                Imgproc.warpPerspective(result, single_box, trans, new Size(final_size, final_size));
                // System.out.println(single_box.dump());
                Imgproc.threshold(single_box, single_box, 200, 255, Imgproc.THRESH_BINARY);
                
                // crop to remove black borders
                Mat single_box_cropped = new Mat(single_box, new Range(14, 84), new Range(14, 84));
                
                // basic blank filter
                // % white > 95 means blank cell
                int white = Core.countNonZero(single_box_cropped);
                double perc_white = white / 4900.0 * 100; // System.out.println(perc_white);
                if (perc_white > 95) {
                    continue;
                }
                
                Imgcodecs.imwrite("images/digit.png", single_box_cropped);
                sudoku_matrix[i][j] = SingleDigitOCR.getDigit("images/digit.png"); 
                // i = j = 10;
            }
        }
        
        // System.out.println(Arrays.deepToString(sudoku_matrix));

        
        SudokuSolver solver = new SudokuSolver(sudoku_matrix);
        System.out.println("Original Matrix: \n" + solver);
        boolean solved = solver.solve();
        if (solved) {
            System.out.println("Solved Matrix: \n" + solver);
        }
        else {
            System.out.println("Cannot solve");
        }
    }
}
