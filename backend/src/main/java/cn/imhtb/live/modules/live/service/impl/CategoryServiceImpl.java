package cn.imhtb.live.modules.live.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.modules.live.service.ICategoryService;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.vo.response.CategoryResp;
import cn.imhtb.live.common.utils.CovertBeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author PinTeh
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Override
    public PageData<CategoryResp> queryCategoryPage(Integer page, Integer limit) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        Page<Category> pageParam = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<Category> wrapper = enabledCategoryWrapper();
        Page<Category> categoryPage = page(pageParam, wrapper);
        List<CategoryResp> categoryRespList = CovertBeanUtil.covertList(categoryPage.getRecords(), CategoryResp.class);
        PageData<CategoryResp> pageData = new PageData<>();
        pageData.setTotal(categoryPage.getTotal());
        pageData.setList(categoryRespList);
        return pageData;
    }

    @Override
    public List<CategoryResp> listEnabledCategories() {
        return CovertBeanUtil.covertList(list(enabledCategoryWrapper()), CategoryResp.class);
    }

    private LambdaQueryWrapper<Category> enabledCategoryWrapper() {
        return new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, StatusEnum.YES.getCode())
                .orderByDesc(Category::getSort)
                .orderByAsc(Category::getId);
    }

}
