package cn.imhtb.live.mappers;

import cn.imhtb.live.pojo.database.Report;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    @Select({
            "<script>",
            "select rp.*,",
            "       r.title as room_title,",
            "       r.disabled as room_disabled,",
            "       r.status as room_status",
            "from report rp",
            "left join room r on rp.room_id = r.id",
            "<where>",
            "  <if test='status != null'>and rp.status = #{status}</if>",
            "  <if test='targetType != null and targetType != \"\"'>and rp.target_type = #{targetType}</if>",
            "</where>",
            "<choose>",
            "  <when test='status != null and status == 0'>order by rp.create_time asc</when>",
            "  <otherwise>order by rp.create_time desc</otherwise>",
            "</choose>",
            "</script>"
    })
    Page<Report> pageForAdmin(Page<Report> page,
                              @Param("status") Integer status,
                              @Param("targetType") String targetType);
}
