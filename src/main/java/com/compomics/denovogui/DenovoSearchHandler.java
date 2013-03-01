/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui;

import com.compomics.util.experiment.identification.SearchParameters;

/**
 * This handle the searches with the search parameters taken from the GUI or the command line
 *
 * @author Marc
 */
public class DenovoSearchHandler {
    
    /**
     * The search parameters
     */
    private SearchParameters searchParameters;
    /**
     * Constructor 
     * @param searchParameters the search parameters
     */
    public DenovoSearchHandler(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
    }
    
    
}
