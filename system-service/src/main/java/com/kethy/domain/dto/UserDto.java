package com.kethy.domain.dto;

import lombok.*;

import java.util.List;

/**
 * @author andylau 2023/5/10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserDto {

    private Long id;

    private String username;

    private List<String> authorities;

}
