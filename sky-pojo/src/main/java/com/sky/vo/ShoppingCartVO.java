package com.sky.vo;

// ... existing code ...
import com.sky.entity.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
// ... existing code ...
@Data
@AllArgsConstructor
@NoArgsConstructor
// ... existing code ...
public class ShoppingCartVO extends ShoppingCart {

    private String categoryName;
    private Integer dishCopies;
}
