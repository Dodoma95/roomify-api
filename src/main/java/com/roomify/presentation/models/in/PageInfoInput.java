package com.roomify.presentation.models.in;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageInfoInput {

    private Integer page;
    private Integer pageSize;
}
