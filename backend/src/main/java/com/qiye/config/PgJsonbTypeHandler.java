package com.qiye.config;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PostgreSQL jsonb 专用 TypeHandler。
 * <p>JacksonTypeHandler 默认以 ps.setString 绑定参数，PG jsonb 列不接受 varchar 表达式，
 * 会报 "column is of type jsonb but expression is of type character varying"。
 * 这里改用 PGobject(type=jsonb) 绑定；读取/反序列化逻辑完全复用父类。</p>
 */
public class PgJsonbTypeHandler extends JacksonTypeHandler {

    public PgJsonbTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        try {
            PGobject json = new PGobject();
            json.setType("jsonb");
            json.setValue(toJson(parameter));
            ps.setObject(i, json);
        } catch (Exception e) {
            throw new SQLException("jsonb 序列化失败: " + e.getMessage(), e);
        }
    }
}
