package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.mappers.CategoryMapper;
import cn.imhtb.live.modules.live.service.impl.CategoryServiceImpl;
import cn.imhtb.live.pojo.database.Category;
import cn.imhtb.live.pojo.vo.response.CategoryResp;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 分类服务")
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private List<Category> sampleCategories;

    @BeforeEach
    void setUp() {
        sampleCategories = Arrays.asList(
                createCategory(1, "游戏", 100),
                createCategory(2, "娱乐", 90),
                createCategory(3, "技术", 80)
        );
    }

    // ─── Story: 获取分类列表 ───────────────────────────────

    @Nested
    @DisplayName("Story: 游客查看直播分类列表")
    class QueryCategoryPage {

        @Test
        @DisplayName("Given: 分类数据存在, When: 查询第1页, Then: 返回按排序降序的分类列表")
        void shouldReturnCategoriesOrderedBySort() {
            Page<Category> mockPage = new Page<>(1, 10);
            mockPage.setRecords(sampleCategories);
            mockPage.setTotal(3);
            when(categoryMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            PageData<CategoryResp> result = categoryService.queryCategoryPage(1, 10);

            assertNotNull(result);
            assertEquals(3, result.getTotal());
            assertEquals(3, result.getList().size());
            assertEquals("游戏", result.getList().get(0).getName());
            assertEquals("娱乐", result.getList().get(1).getName());
            assertEquals("技术", result.getList().get(2).getName());
        }

        @Test
        @DisplayName("Given: 无分类数据, When: 查询, Then: 返回空列表")
        void shouldReturnEmptyWhenNoCategories() {
            Page<Category> emptyPage = new Page<>(1, 10);
            emptyPage.setRecords(Arrays.asList());
            emptyPage.setTotal(0);
            when(categoryMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(emptyPage);

            PageData<CategoryResp> result = categoryService.queryCategoryPage(1, 10);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getList().isEmpty());
        }

        @Test
        @DisplayName("Given: 多页数据, When: 查询第2页, Then: 返回正确的分页数据")
        void shouldPaginateCorrectly() {
            Page<Category> page2 = new Page<>(2, 2);
            page2.setRecords(sampleCategories.subList(2, 3));
            page2.setTotal(3);
            when(categoryMapper.selectPage(eq(new Page<>(2, 2)), any(LambdaQueryWrapper.class)))
                    .thenReturn(page2);

            PageData<CategoryResp> result = categoryService.queryCategoryPage(2, 2);

            assertEquals(3, result.getTotal());
            assertEquals(1, result.getList().size());
            assertEquals("技术", result.getList().get(0).getName());
        }
    }

    // ─── Helper ────────────────────────────────────────────

    private Category createCategory(Integer id, String name, Integer sort) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSort(sort);
        category.setStatus(1);
        category.setParentId(0);
        return category;
    }
}
