package io.github.pmcarpan.sudoku;

import java.io.File;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class SingleDigitOCR {
    private static ITesseract instance;

    static {
        instance = new Tesseract();
        instance.setLanguage("eng");
        instance.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_CHAR);
        instance.setTessVariable("tessedit_char_whitelist", "0123456789");
    }
    
    // do not instantiate
    private SingleDigitOCR() {
    }

    // expected to return digit between 1 to 9 (both inclusive)
    // throws IllegalArgumentExcepton if filePath is null
    // throws IllegalStateException if OCR fails
    // throws IllegalStateException if a digit between 1 to 9 is not detected
    public static int getDigit(String filePath) {
        if (filePath == null) 
            throw new IllegalArgumentException("File path is null");

        File imageFile = new File(filePath);

        String result = null;

        try {
            result = instance.doOCR(imageFile);
        } 
        catch (TesseractException e) {
            System.out.println("Error while performing OCR. Message: " + e.getMessage());
            e.printStackTrace();
        }

        if (result == null || result.length() == 0)
            throw new IllegalStateException("Could not detect anything");

        // System.out.println("Detected: " + result);

        int digit = result.charAt(0); // remove the spaces at the end

        if (digit >= '1' && digit <= '9') 
            return digit - '0';
        
        throw new IllegalStateException("Could not detect digit between 1 to 9." + 
                                        " Detected UNICODE: " + digit);
    }

}
