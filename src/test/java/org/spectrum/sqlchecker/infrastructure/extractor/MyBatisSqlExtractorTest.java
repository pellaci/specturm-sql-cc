package org.spectrum.sqlchecker.infrastructure.extractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MyBatisSqlExtractor 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MyBatisSqlExtractor 单元测试")
class MyBatisSqlExtractorTest {

    private MyBatisSqlExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MyBatisSqlExtractor();
    }

    // ==================== 基本属性测试 ====================

    @Nested
    @DisplayName("基本属性测试")
    class BasicPropertiesTests {

        @Test
        @DisplayName("应该返回正确的名称")
        void should_return_correct_name() {
            assertThat(extractor.getName()).isEqualTo("MyBatisSqlExtractor");
        }

        @Test
        @DisplayName("应该返回 MYBATIS 源类型")
        void should_return_mybatis_source_type() {
            assertThat(extractor.getSourceType()).isEqualTo(SqlSourceType.MYBATIS);
        }

        @Test
        @DisplayName("应该支持 XML 文件类型")
        void should_support_xml_file_type() {
            FileType xmlType = FileType.fromPath("test.xml");
            assertThat(extractor.supports(xmlType)).isTrue();
        }

        @Test
        @DisplayName("应该支持 XML 文件类型（大写扩展名）")
        void should_support_xml_file_type_uppercase() {
            FileType xmlType = FileType.fromPath("test.XML");
            assertThat(extractor.supports(xmlType)).isTrue();
        }

        @Test
        @DisplayName("不应该支持非 XML 文件类型")
        void should_not_support_non_xml_file_type() {
            FileType javaType = FileType.fromPath("test.java");
            FileType jsType = FileType.fromPath("test.js");
            FileType txtType = FileType.fromPath("test.txt");

            assertThat(extractor.supports(javaType)).isFalse();
            assertThat(extractor.supports(jsType)).isFalse();
            assertThat(extractor.supports(txtType)).isFalse();
        }
    }

    // ==================== extract 方法测试 ====================

    @Nested
    @DisplayName("extract 方法测试")
    class ExtractTests {

        @Test
        @DisplayName("应该提取 select 元素中的 SQL")
        void should_extract_sql_from_select_element() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectAll">
                        SELECT id, name FROM users
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("SELECT");
            assertThat(result.get(0)).contains("users");
        }

        @Test
        @DisplayName("应该提取 insert 元素中的 SQL")
        void should_extract_sql_from_insert_element() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <insert id="insertUser">
                        INSERT INTO users (id, name) VALUES (1, 'test')
                    </insert>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("INSERT");
        }

        @Test
        @DisplayName("应该提取 update 元素中的 SQL")
        void should_extract_sql_from_update_element() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <update id="updateUser">
                        UPDATE users SET name = 'test' WHERE id = 1
                    </update>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("UPDATE");
        }

        @Test
        @DisplayName("应该提取 delete 元素中的 SQL")
        void should_extract_sql_from_delete_element() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <delete id="deleteUser">
                        DELETE FROM users WHERE id = 1
                    </delete>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("DELETE");
        }

        @Test
        @DisplayName("应该提取 sql 元素中的 SQL 片段")
        void should_extract_sql_from_sql_element() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <sql id="userColumns">
                        id, name, email
                    </sql>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            // sql 元素不包含 SQL 关键字，所以不会被提取
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该提取多个 SQL 元素")
        void should_extract_multiple_sql_elements() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectAll">
                        SELECT * FROM users
                    </select>
                    <select id="selectById">
                        SELECT * FROM users WHERE id = #{id}
                    </select>
                    <insert id="insert">
                        INSERT INTO users (name) VALUES (#{name})
                    </insert>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("应该返回 MyBatis statement 的真实起始行号")
        void should_return_real_statement_start_lines() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <mapper namespace="com.example.UserMapper">

                    <select id="selectAll">
                        SELECT id, name FROM users
                    </select>

                    <insert id="insertUser">
                        INSERT INTO users (id, name) VALUES (#{id}, #{name})
                    </insert>
                </mapper>
                """;

            List<MyBatisSqlExtractor.LocatedSql> result = extractor.extractWithLocations(xml);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).sql()).contains("SELECT id, name FROM users");
            assertThat(result.get(0).line()).isEqualTo(4);
            assertThat(result.get(1).sql()).contains("INSERT INTO users");
            assertThat(result.get(1).line()).isEqualTo(8);
        }

        @Test
        @DisplayName("应该处理 CDATA 中的 SQL")
        void should_handle_cdata_sql() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectAll">
                        <![CDATA[
                            SELECT * FROM users WHERE status = 'active'
                        ]]>
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("SELECT");
            assertThat(result.get(0)).contains("active");
        }

        @Test
        @DisplayName("应该处理带动态 SQL 的语句")
        void should_handle_dynamic_sql() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectByCondition">
                        SELECT * FROM users
                        <where>
                            <if test="name != null">
                                AND name LIKE #{name}
                            </if>
                            <if test="status != null">
                                AND status = #{status}
                            </if>
                        </where>
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("SELECT");
        }

        @Test
        @DisplayName("应该处理 foreach 元素")
        void should_handle_foreach_element() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectByIds">
                        SELECT * FROM users WHERE id IN
                        <foreach item="id" collection="ids" open="(" separator="," close=")">
                            #{id}
                        </foreach>
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(normalizeSql(result.get(0))).contains("IN (#{id})");
        }

        @Test
        @DisplayName("应该处理 trim 元素")
        void should_handle_trim_element() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <update id="updateUser">
                        UPDATE users
                        <trim prefix="SET" suffixOverrides=",">
                            <if test="name != null">name = #{name},</if>
                            <if test="email != null">email = #{email},</if>
                        </trim>
                        WHERE id = #{id}
                    </update>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("UPDATE");
            assertThat(result.get(0)).contains("SET");
            assertThat(result.get(0)).doesNotContain(", WHERE");
        }

        @Test
        @DisplayName("应该处理 trim 的 prefixOverrides")
        void should_handle_trim_prefix_overrides() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectByStatus">
                        SELECT * FROM users
                        <trim prefix="WHERE" prefixOverrides="AND|OR">
                            AND status = #{status}
                        </trim>
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(normalizeSql(result.get(0))).contains("WHERE status = #{status}");
        }

        @Test
        @DisplayName("应该提取带 selectKey 元素的 SQL")
        void should_extract_sql_with_selectkey() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <insert id="insertUser">
                        <selectKey keyProperty="id" resultType="long" order="BEFORE">
                            SELECT NEXTVAL('user_seq')
                        </selectKey>
                        INSERT INTO users (id, name) VALUES (#{id}, #{name})
                    </insert>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            // selectKey 和 insert 会分别被提取
            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(sql -> sql.contains("SELECT NEXTVAL"));
            assertThat(result).anyMatch(sql -> sql.contains("INSERT INTO users"));
        }

        @Test
        @DisplayName("应该保留 foreach 内部 CASE WHEN 片段")
        void should_extract_foreach_body_for_case_updates() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.NativeConfigMapper">
                    <update id="sortNativeIndex" parameterType="java.util.Map">
                        UPDATE s_native_config_index
                        <trim prefix="SET sort = CASE index_id" suffix="END">
                            <foreach item="value" index="key" collection="indexMap">
                                WHEN #{key} THEN #{value}
                            </foreach>
                        </trim>
                        WHERE
                        <foreach item="value" index="key" collection="indexMap" separator="or">
                            index_id = #{key}
                        </foreach>
                    </update>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            String sql = normalizeSql(result.get(0));
            assertThat(sql).contains("CASE index_id WHEN #{key} THEN #{value} END");
            assertThat(sql).contains("WHERE index_id = #{key}");
            assertThat(sql).doesNotContain("CASE index_id 1 END");
        }

        @Test
        @DisplayName("应该保留嵌套 trim/foreach/if 中的 CASE WHEN 片段")
        void should_extract_nested_trim_foreach_if_case_body() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.SkuMapper">
                    <update id="batchUpdatePrices" parameterType="list">
                        update s_sku
                        <trim prefix="set" suffixOverrides=",">
                            <trim prefix="jd_price =case" suffix="end,">
                                <foreach collection="list" item="c" index="index">
                                    <if test="c.jdPrice != null">
                                        when jd_sku_id = #{c.jdSkuId} then #{c.jdPrice}
                                    </if>
                                </foreach>
                            </trim>
                        </trim>
                        where
                        <foreach collection="list" separator="or" item="c" index="index">
                            jd_sku_id = #{c.jdSkuId}
                        </foreach>
                    </update>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            String sql = normalizeSql(result.get(0));
            assertThat(sql).contains("jd_price =case when jd_sku_id = #{c.jdSkuId} then #{c.jdPrice} end");
            assertThat(sql).doesNotContain("jd_price =case 1 end");
        }

        @Test
        @DisplayName("应该保留 foreach 内部批量 insert values 片段")
        void should_extract_foreach_body_for_batch_insert_values() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.SkuMapper">
                    <insert id="insertSkuList" parameterType="java.util.List">
                        INSERT INTO s_sku (id, name)
                        VALUES
                        <foreach collection="list" item="item" separator=",">
                            (#{item.id}, #{item.name})
                        </foreach>
                    </insert>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(normalizeSql(result.get(0)))
                    .contains("VALUES (#{item.id}, #{item.name})");
        }

        @Test
        @DisplayName("应该忽略空的 SQL 元素")
        void should_ignore_empty_sql_elements() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectAll">

                    </select>
                    <select id="selectById">
                        SELECT * FROM users WHERE id = #{id}
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该修剪提取的 SQL")
        void should_trim_extracted_sql() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectAll">
                        SELECT * FROM users
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result.get(0)).doesNotStartWith(" ");
            assertThat(result.get(0)).doesNotEndWith(" ");
        }

        @Test
        @DisplayName("应该解析 include 引用片段")
        void should_resolve_include_fragments() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <sql id="Base_Column_List">
                        id, name
                    </sql>
                    <select id="selectAll">
                        <include refid="Base_Column_List"/>
                        FROM users
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(normalizeSql(result.get(0))).contains("SELECT id, name FROM users");
        }

        @Test
        @DisplayName("应该优先选择 choose 的第一个 when")
        void should_pick_first_when_in_choose() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectByChoose">
                        SELECT * FROM users
                        <where>
                            <choose>
                                <when test="name != null">AND name = #{name}</when>
                                <otherwise>AND status = 1</otherwise>
                            </choose>
                        </where>
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("WHERE name = #{name}");
            assertThat(result.get(0)).doesNotContain("status = 1");
        }

        @Test
        @DisplayName("应该为 select 语句自动补全 SELECT 前缀")
        void should_prefix_select_keyword_when_missing() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <select id="selectColumns">
                        id, name FROM users
                    </select>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).hasSize(1);
            assertThat(normalizeSql(result.get(0))).startsWith("SELECT ");
        }
    }

    // ==================== 错误处理测试 ====================

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("应该处理无效的 XML")
        void should_handle_invalid_xml() throws SqlExtractionException {
            String invalidXml = "<invalid><unclosed>";

            List<String> result = extractor.extract(invalidXml);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该处理空输入")
        void should_handle_empty_input() throws SqlExtractionException {
            List<String> result = extractor.extract("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该处理 null 输入")
        void should_handle_null_input() {
            assertThatThrownBy(() -> extractor.extract(null))
                    .isInstanceOf(SqlExtractionException.class);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("应该处理只有注释的 XML")
        void should_handle_xml_with_only_comments() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <!-- This is a comment -->
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该处理混合大小写的元素名")
        void should_handle_mixed_case_element_names() throws SqlExtractionException {
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="com.example.UserMapper">
                    <Select id="selectAll">
                        SELECT * FROM users
                    </Select>
                    <SELECT id="selectAllUpper">
                        SELECT * FROM users
                    </SELECT>
                </mapper>
                """;

            List<String> result = extractor.extract(xml);

            // 元素名转换为小写比较
            assertThat(result).hasSize(2);
        }
    }

    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }
}
