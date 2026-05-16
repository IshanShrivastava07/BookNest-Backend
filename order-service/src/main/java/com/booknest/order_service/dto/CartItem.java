package com.booknest.order_service.dto;

import lombok.Data;

@Data
public class CartItem {
	 private Long itemId;
	    private Long cartId;
	    private Long bookId;
	    private String bookTitle;
	    private double price;
	    private int quantity;
	    public String coverImage;
	    public String author;
	    
	    

}
