package com.neu;

import com.neu.constant.JwtClaimsConstant;
import com.neu.properties.JwtProperties;
import com.neu.utils.JwtUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SkyApplication.class)
@AutoConfigureMockMvc
class ReportExportTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProperties jwtProperties;

    @Test
    void exportUsesRequestedDateRange() throws Exception {
        LocalDate begin = LocalDate.now().minusDays(6);
        LocalDate end = LocalDate.now();
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                Map.of(JwtClaimsConstant.MERCHANT_ID, 1L,
                        JwtClaimsConstant.ROLE, "ADMIN")
        );

        MockHttpServletResponse response = mockMvc.perform(get("/admin/report/export")
                        .servletPath("/admin/report/export")
                        .header("token", token)
                        .param("begin", begin.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertTrue(response.getContentType().startsWith(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertTrue(response.getHeader("Content-Disposition").contains(begin + "_" + end));

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("统计时间：" + begin + " 至 " + end,
                    workbook.getSheet("运营数据").getRow(1).getCell(0).getStringCellValue());
        }
    }
}
