/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import org.junit.jupiter.api.Test;
import ru.beeline.fdmbpm.dto.product.ProductInfoShortDTO;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetProductsForFfRecalcDelegateTest {

    @Test
    void keepsAliasWithStructurizrUrl() {
        ProductInfoShortDTO withUrl = new ProductInfoShortDTO();
        withUrl.setAlias(" app ");
        withUrl.setStructurizrApiUrl(" https://structurizr ");
        ProductInfoShortDTO withoutUrl = new ProductInfoShortDTO();
        withoutUrl.setAlias("empty");
        withoutUrl.setStructurizrApiUrl("  ");
        ProductInfoShortDTO withoutAlias = new ProductInfoShortDTO();
        withoutAlias.setAlias(" ");
        withoutAlias.setStructurizrApiUrl("https://structurizr");

        assertEquals(List.of("app"), GetProductsForFfRecalcDelegate.aliasesWithStructurizr(Arrays.asList(
                withUrl, withoutUrl, withoutAlias, null)));
        assertEquals(List.of(), GetProductsForFfRecalcDelegate.aliasesWithStructurizr(null));
        assertEquals(List.of(), GetProductsForFfRecalcDelegate.aliasesWithStructurizr(List.of()));
    }
}
