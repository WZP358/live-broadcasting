package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.modules.live.service.impl.CategoryServiceImpl;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.vo.response.CategoryResp;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 分类服务")
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl();
        ReflectionTestUtils.setField(categoryService, "baseMapper", categoryMapper);
    }

    @Test
    @DisplayName("分页查询只使用启用分类，并按排序降序、ID升序")
    void shouldQueryEnabledCategoriesWithStableOrder() {
        Page<Category> mockPage = new Page<>(1, 20);
        mockPage.setRecords(List.of(
                createCategory(2, "娱乐", 90),
                createCategory(1, "游戏", 80)
        ));
        mockPage.setTotal(2);
        when(categoryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(mockPage);

        PageData<CategoryResp> result = categoryService.queryCategoryPage(0, 0);

        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(List.of("娱乐", "游戏"), result.getList().stream().map(CategoryResp::getName).toList());

        ArgumentCaptor<Page<Category>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Wrapper<Category>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(categoryMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertEquals(1, pageCaptor.getValue().getCurrent());
        assertEquals(20, pageCaptor.getValue().getSize());
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    @DisplayName("列表查询复用启用分类口径")
    void shouldListEnabledCategories() {
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                createCategory(1, "游戏", 100),
                createCategory(3, "技术", 80)
        ));

        List<CategoryResp> result = categoryService.listEnabledCategories();

        assertEquals(2, result.size());
        assertEquals("游戏", result.get(0).getName());

        ArgumentCaptor<Wrapper<Category>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(categoryMapper).selectList(wrapperCaptor.capture());
        assertNotNull(wrapperCaptor.getValue());
    }

    private Category createCategory(Integer id, String name, Integer sort) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSort(sort);
        category.setStatus(StatusEnum.YES.getCode());
        category.setParentId(0);
        return category;
    }
}
