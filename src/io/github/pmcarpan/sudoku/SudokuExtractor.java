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

public class SudokuExtractor {
    private int[][] extractedArray;
    
    public SudokuExtractor(String filePath) {
        if (filePath == null)
            throw new IllegalArgumentException("file path is NULL");
        
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Mat sudoku = Imgcodecs.imread(filePath, 0);
        Mat preprocessedImage = new Mat();
        preprocess(sudoku, preprocessedImage);
        
        Mat mask = new Mat();
        generateMask(preprocessedImage, mask);
        
        // Imgcodecs.imwrite("images/processed-image.png", preprocessedImage);
        // Imgcodecs.imwrite("images/mask.png", mask);
        
        Mat verticalLines = new Mat(),
                horizontalLines = new Mat();
        generateGridLines(preprocessedImage, mask, verticalLines, 1, 0, 3, 13);
        generateGridLines(preprocessedImage, mask, horizontalLines, 0, 1, 13, 3);
        
        // Imgcodecs.imwrite("images/thresh-vertical.png", verticalLines);
        // Imgcodecs.imwrite("images/thresh-horizontal.png", horizontalLines);
        
        Mat intersections = new Mat();
        getIntersections(verticalLines, horizontalLines, intersections);
        
        // Imgcodecs.imwrite("images/intersections.png", intersections);
        
        Point[] intersectionsArray = getIntersectionPoints(intersections);
        
        // Imgcodecs.imwrite("images/intersections.png", intersections);
        
        int[][] sudokuArray = getSudokuArray(preprocessedImage, intersectionsArray);
        
        extractedArray = sudokuArray;
    }

    // divide image by result of closing operation
    // then normalize
    private void preprocess(Mat sudoku, Mat preprocessedImage) {
        Mat closing = new Mat(),
                kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(11, 11));
        
        Imgproc.morphologyEx(sudoku, closing, Imgproc.MORPH_CLOSE, kernel);

        Core.divide(sudoku, closing, preprocessedImage, 1.0, CvType.CV_64F);

        Core.normalize(preprocessedImage, preprocessedImage, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);
    }
    
    // generate a mask for the sudoku area
    private void generateMask(Mat preprocessedImage, Mat mask) {
        Mat blur = new Mat(), 
                thresh = new Mat();
        Imgproc.GaussianBlur(preprocessedImage, blur, new Size(11, 11), 0);
        Imgproc.adaptiveThreshold(blur, thresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);
        
        Mat outerFrame = new Mat();
        getOuterFrame(thresh, outerFrame);
        
        Core.bitwise_not(outerFrame, mask);

        Imgproc.floodFill(outerFrame, new Mat(), new Point(0, 0), new Scalar(0));

        Core.bitwise_or(mask, outerFrame, mask);
    }
    
    // get the outer frame of the grid
    private void getOuterFrame(Mat thresh, Mat outerFrame) {
        Point maxPoint = getMaxPoint(thresh);
        Mat temp = thresh.clone();

        // fill the max area grey
        Imgproc.floodFill(temp, new Mat(), maxPoint, new Scalar(150));
        
        // white out the remaining black areas
        for (int y = 0; y < temp.size().height; y++) {
            for (int x = 0; x < temp.size().width; x++) {
                if (temp.get(y, x)[0] < 128) {
                    Imgproc.floodFill(temp, new Mat(), new Point(x, y), new Scalar(255));
                }
            }
        }
        
        // fill the max area black
        Imgproc.floodFill(temp, new Mat(), maxPoint, new Scalar(0));
        
        temp.copyTo(outerFrame);
    }
    
    // get a point in the connected component with max area
    private Point getMaxPoint(Mat thresh) {
        int maxArea = -1;
        Point maxPoint = null;
        Mat temp = thresh.clone();

        for (int y = 0; y < temp.rows(); y++) {
            for (int x = 0; x < temp.cols(); x++) {
                if (temp.get(y, x)[0] < 128) {
                    int area = Imgproc.floodFill(temp, new Mat(), 
                                                    new Point(x, y), new Scalar(130));
                    if (area > maxArea) {
                        maxPoint = new Point(x, y);
                        maxArea = area;
                    }
                }
            }
        }
        
        return maxPoint;
    }
    
    // generate the grid lines using sobel operator
    private void generateGridLines(Mat preprocessedImage, Mat mask, Mat dst, int degreeX, int degreeY, int kerX, int kerY) {
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kerX, kerY));
        Mat diff = new Mat();
        Imgproc.Sobel(preprocessedImage, diff, CvType.CV_16S, degreeX, degreeY);
        Core.convertScaleAbs(diff, diff);
        Core.normalize(diff, diff, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);
        Core.bitwise_and(diff, mask, diff);
        Imgproc.GaussianBlur(diff, diff, new Size(7, 7), 2);
        Imgproc.morphologyEx(diff, diff, Imgproc.MORPH_CLOSE, kernel);
        
        // if (degreeX == 1)
        //     Imgcodecs.imwrite("images/sobel-dx.png", diff);
        // else
        //     Imgcodecs.imwrite("images/sobel-dy.png", diff);
        
        Mat thresh = new Mat();
        Imgproc.threshold(diff, thresh, 0, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_OPEN, kernel);
        
        // if (degreeX == 1)
        //     Imgcodecs.imwrite("images/actual-thresh-dx.png", thresh);
        // else
        //     Imgcodecs.imwrite("images/actual-thresh-dy.png", thresh);
        
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresh, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // sort contours by height or width
        if (degreeX == 1) // vertical lines, sort by height
            contours.sort( (c1, c2) -> -Double.compare( Imgproc.boundingRect(c1).height, Imgproc.boundingRect(c2).height ) );
        else // horizontal lines, sort by width
            contours.sort( (c1, c2) -> -Double.compare( Imgproc.boundingRect(c1).width, Imgproc.boundingRect(c2).width ) );
        
        for (int i = 0; i < contours.size(); i++) {
            if (i < 10) // draw largest 10 contours
                Imgproc.drawContours(thresh, contours, i, new Scalar(255), -1);
            else
                Imgproc.drawContours(thresh, contours, i, new Scalar(0), -1);
        }

        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.dilate(thresh, thresh, kernel);
        
        thresh.copyTo(dst);
    }
    
    // get intersections by AND-ing with grid lines
    private void getIntersections(Mat verticalLines, Mat horizontalLines, Mat intersections) {
        Core.bitwise_and(verticalLines, horizontalLines, intersections);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(intersections, intersections, Imgproc.MORPH_CLOSE, kernel);
    }
    
    // get the intersection points
    private Point[] getIntersectionPoints(Mat intersections) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(intersections, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        List<Point> intersectionPoints = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Moments m = Imgproc.moments(contour);
            double x = m.m10 / m.m00, 
                    y = m.m01 / m.m00;
            intersectionPoints.add(new Point(x, y));
        }

        Point[] intersectionPointsArray = intersectionPoints.toArray(new Point[0]);
        
        if (intersectionPointsArray.length != 100)
            throw new IllegalStateException("Detected " + (intersectionPointsArray.length) + " points. Required 100.");
            
        // sort by y coordinate first
        // then sort groups of 10 by x coordinate
        Arrays.sort(intersectionPointsArray, (p1, p2) -> (Double.compare(p1.y, p2.y)));
        for (int i = 0; i < 100; i += 10) {
            Arrays.sort(intersectionPointsArray, i, i+10, (p1, p2) -> (Double.compare(p1.x, p2.x)));
        }
        
        // System.out.println(intersectionPointsArray.length);
        
        // int cnt = 0;
        // for (Point p : intersectionPointsArray) {
        //     Imgproc.putText(intersections, (cnt++)+"", p, 0, 0.5, new Scalar(128));
        // }
        
        return intersectionPointsArray;
    }
    
    // create the 2D sudoku matrix
    private int[][] getSudokuArray(Mat preprocessedImage, Point[] intersectionsArray) {
        int finalSize = 100; // final size of perspective transform square box
        
        List<Point> finalCorners = new ArrayList<>();
        finalCorners.add(new Point(0, 0));
        finalCorners.add(new Point(finalSize, 0));
        finalCorners.add(new Point(0, finalSize));
        finalCorners.add(new Point(finalSize, finalSize));
        Mat finalCornersMat = Converters.vector_Point2f_to_Mat(finalCorners);

        int[][] sudokuMatrix = new int[9][9];
        
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int index = i * 10 + j;
                
                List<Point> actualCorners = new ArrayList<>();
                actualCorners.add(intersectionsArray[index]);
                actualCorners.add(intersectionsArray[index + 1]);
                actualCorners.add(intersectionsArray[index + 10]);
                actualCorners.add(intersectionsArray[index + 11]);
                Mat actualCornersMat = Converters.vector_Point2f_to_Mat(actualCorners);

                // the perspective transform
                Mat trans = Imgproc.getPerspectiveTransform(actualCornersMat, finalCornersMat);

                Mat singleBox = new Mat();
                
                // perspective transform to a 100 x 100 square
                Imgproc.warpPerspective(preprocessedImage, singleBox, trans, new Size(finalSize, finalSize));
                
                // Imgcodecs.imwrite("images/single-box.png", singleBox);
                
                Imgproc.threshold(singleBox, singleBox, 200, 255, Imgproc.THRESH_BINARY);
                                
                // crop to remove black borders
                Mat singleBoxCropped = new Mat(singleBox, new Range(14, 84), new Range(14, 84));
                
                // basic blank filter
                // % white > 95 means blank cell
                int white = Core.countNonZero(singleBoxCropped);
                double percentageWhite = white / 4900.0 * 100;
                if (percentageWhite > 95) {
                    continue;
                }
                
                // System.out.println(percentageWhite);
                // System.out.println(intersectionsArray[index]);
                // System.out.println(intersectionsArray[index+1]);
                // System.out.println(intersectionsArray[index+10]);
                // System.out.println(intersectionsArray[index+11]);
                
                Imgcodecs.imwrite("images/digit.png", singleBoxCropped);
                
                // run OCR
                sudokuMatrix[i][j] = SingleDigitOCR.getDigit("images/digit.png");
            }
        }
        
        return sudokuMatrix;
    }
    
    public int[][] getExtractedArray() {
        return extractedArray;
    }
    
}
