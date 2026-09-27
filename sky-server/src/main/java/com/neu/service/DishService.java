package com.neu.service;

import com.neu.dto.DishDTO;
import com.neu.dto.DishPageQueryDTO;
import com.neu.result.PageResult;
import com.neu.vo.DishVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DishService {

    PageResult page(DishPageQueryDTO query, Long merchantId);

    List<DishVO> listForUser(Long categoryId);

    DishVO getByIdForUser(Long id);

    DishVO getById(Long id, Long merchantId);

    void save(DishDTO dishDTO, Long merchantId);

    void update(DishDTO dishDTO, Long merchantId);

    void updateStatus(Integer status, Long id, Long merchantId);

    void delete(Long id, Long merchantId);


    String uploadImage(MultipartFile file);
}
