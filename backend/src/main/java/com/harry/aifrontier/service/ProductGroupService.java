package com.harry.aifrontier.service;

/**
 * Pipeline C: group product-update candidates by product.
 */
public interface ProductGroupService {
    /**
     * Auto-group pending product candidates by product name/brand.
     * Returns the number of groups created or updated.
     */
    int autoGroup();
}
