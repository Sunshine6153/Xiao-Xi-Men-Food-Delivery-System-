package com.neu.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neu.dto.DishDTO;
import com.neu.dto.DishFlavorDTO;
import com.neu.dto.DishPageQueryDTO;
import com.neu.entity.Category;
import com.neu.entity.Dish;
import com.neu.entity.DishFlavor;
import com.neu.exception.MerchantBusinessException;
import com.neu.mapper.CategoryMapper;
import com.neu.mapper.DishFlavorMapper;
import com.neu.mapper.DishMapper;
import com.neu.properties.LocalFileProperties;
import com.neu.result.PageResult;
import com.neu.service.DishService;
import com.neu.vo.DishVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private LocalFileProperties localFileProperties;


    @Override
    public PageResult page(DishPageQueryDTO query, Long merchantId) {
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        if (merchantId != null) {
            query.setMerchantId(merchantId);
        }
        PageHelper.startPage(page, pageSize);

        List<DishVO> dishes = dishMapper.pageQuery(query);
        for (DishVO dish : dishes) {
            dish.setFlavors(dishFlavorMapper.listByDishId(dish.getId()));
        }
        PageInfo<DishVO> pageInfo = new PageInfo<>(dishes);
        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public List<DishVO> listForUser(Long categoryId) {
        List<DishVO> dishes = dishMapper.listForUser(categoryId);
        for (DishVO dish : dishes) {
            dish.setFlavors(dishFlavorMapper.listByDishId(dish.getId()));
        }
        return dishes;
    }

    @Override
    public DishVO getByIdForUser(Long id) {
        DishVO dish = dishMapper.getByIdForUser(id);
        if (dish == null) {
            throw new MerchantBusinessException(404, "菜品不存在或暂未上架");
        }
        dish.setFlavors(dishFlavorMapper.listByDishId(id));
        return dish;
    }

    @Override
    public DishVO getById(Long id, Long merchantId) {
        DishVO dish;
        if (merchantId == null) {
            dish = dishMapper.getByIdForAdmin(id);
        } else {
            Dish condition = Dish.builder().id(id).merchantId(merchantId).build();
            dish = dishMapper.getByIdForMerchant(condition);
        }
        if (dish == null) {
            throw new MerchantBusinessException(404,
                    merchantId == null ? "菜品不存在" : "菜品不存在或不属于当前商户");
        }
        dish.setFlavors(dishFlavorMapper.listByDishId(id));
        return dish;
    }

    @Override
    @Transactional
    public void save(DishDTO dishDTO, Long merchantId) {
        Category category = requireCategory(dishDTO.getCategoryId());

        Dish dish = Dish.builder()
                .name(dishDTO.getName())
                .categoryId(category.getId())
                .merchantId(merchantId)
                .price(dishDTO.getPrice())
                .image(dishDTO.getImage())
                .description(dishDTO.getDescription())
                .status(1)
                .build();
        dishMapper.insertDish(dish);
        saveFlavors(dish.getId(), dishDTO.getFlavors());
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO, Long merchantId) {
        Category category = requireCategory(dishDTO.getCategoryId());

        Dish dish = Dish.builder()
                .id(dishDTO.getId())
                .name(dishDTO.getName())
                .categoryId(category.getId())
                .merchantId(merchantId)
                .price(dishDTO.getPrice())
                .image(dishDTO.getImage())
                .description(dishDTO.getDescription())
                .build();
        if (dishMapper.updateDish(dish) == 0) {
            throw new MerchantBusinessException(404, "菜品不存在或不属于当前商户");
        }

        dishFlavorMapper.deleteByDishId(dish.getId());
        saveFlavors(dish.getId(), dishDTO.getFlavors());
    }

    @Override
    public void updateStatus(Integer status, Long id, Long merchantId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new MerchantBusinessException(400, "菜品状态只能为启用或禁用");
        }
        Dish dish = Dish.builder()
                .id(id)
                .merchantId(merchantId)
                .status(status)
                .build();
        int updated = merchantId == null
                ? dishMapper.updateStatusForAdmin(dish)
                : dishMapper.updateStatus(dish);
        if (updated == 0) {
            throw new MerchantBusinessException(404,
                    merchantId == null ? "菜品不存在" : "菜品不存在或不属于当前商户");
        }
    }

    @Override
    @Transactional
    public void delete(Long id, Long merchantId) {
        Dish condition = Dish.builder().id(id).merchantId(merchantId).build();
        if (dishMapper.getByIdForMerchant(condition) == null) {
            throw new MerchantBusinessException(404, "菜品不存在或不属于当前商户");
        }
        dishFlavorMapper.deleteByDishId(id);
        dishMapper.deleteDish(condition);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MerchantBusinessException(400, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get(localFileProperties.getImages());
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(fileName));
        } catch (IOException e) {
            throw new MerchantBusinessException(500, "图片上传失败");
        }
        return "/images/" + fileName;
    }

    private Category requireCategory(Long categoryId) {
        if (categoryId == null) {
            throw new MerchantBusinessException(400, "菜品必须选择分类");
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new MerchantBusinessException(404, "菜品分类不存在");
        }
        if (Integer.valueOf(0).equals(category.getStatus())) {
            throw new MerchantBusinessException(400, "不能选择已禁用的分类");
        }
        return category;
    }

    private void saveFlavors(Long dishId, List<DishFlavorDTO> flavors) {
        if (flavors == null) {
            return;
        }
        for (DishFlavorDTO flavorDTO : flavors) {
            if (flavorDTO.getName() == null || flavorDTO.getName().isBlank()
                    || flavorDTO.getValue() == null || flavorDTO.getValue().isBlank()) {
                throw new MerchantBusinessException(400, "菜品口味名称和值不能为空");
            }
            dishFlavorMapper.insertFlavor(DishFlavor.builder()
                    .dishId(dishId)
                    .name(flavorDTO.getName())
                    .value(flavorDTO.getValue())
                    .build());
        }
    }
}
