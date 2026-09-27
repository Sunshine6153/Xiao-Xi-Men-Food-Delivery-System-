package com.neu;

import com.neu.dto.MerchantLoginDTO;
import com.neu.entity.Merchant;
import com.neu.mapper.MerchantMapper;
import com.neu.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SkyApplication.class)
@Transactional
@Rollback
public class MerchantLoginTest {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private MerchantMapper merchantMapper;

    private static final String TEST_USERNAME = "testmerchant";
    private static final String TEST_PASSWORD = "123456";
    private static final String TEST_MERCHANT_NAME = "Test Merchant";

    @BeforeEach
    public void setUp() {
        // Clear test data - deleteById not available in mapper
        // merchantMapper.deleteById(1L);
    }

    @Test
    public void testMerchantLogin() {
        // 1. Insert test merchant
        String encryptedPassword = DigestUtils.md5DigestAsHex(TEST_PASSWORD.getBytes());
        Merchant testMerchant = Merchant.builder()
                .username(TEST_USERNAME)
                .password(encryptedPassword)
                .merchantName(TEST_MERCHANT_NAME)
                .phone("13800000000")
                .role("MERCHANT")
                .status(1)
                .build();
        merchantMapper.insert(testMerchant);

        // 2. Test successful login
        MerchantLoginDTO loginDTO = MerchantLoginDTO.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();

        Merchant loginResult = merchantService.login(loginDTO);
        assertNotNull(loginResult);
        assertEquals(TEST_USERNAME, loginResult.getUsername());
        assertEquals(TEST_MERCHANT_NAME, loginResult.getMerchantName());
    }

    @Test
    public void testLoginWithWrongPassword() {
        // 1. Insert test merchant
        String encryptedPassword = DigestUtils.md5DigestAsHex(TEST_PASSWORD.getBytes());
        Merchant testMerchant = Merchant.builder()
                .username(TEST_USERNAME)
                .password(encryptedPassword)
                .merchantName(TEST_MERCHANT_NAME)
                .phone("13800000000")
                .role("MERCHANT")
                .status(1)
                .build();
        merchantMapper.insert(testMerchant);

        // 2. Test login with wrong password
        MerchantLoginDTO loginDTO = MerchantLoginDTO.builder()
                .username(TEST_USERNAME)
                .password("wrongpassword")
                .build();

        assertThrows(Exception.class, () -> merchantService.login(loginDTO));
    }

    @Test
    public void testLoginWithNonExistentUser() {
        // Test login with non-existent user
        MerchantLoginDTO loginDTO = MerchantLoginDTO.builder()
                .username("nonexistent")
                .password(TEST_PASSWORD)
                .build();

        assertThrows(Exception.class, () -> merchantService.login(loginDTO));
    }
}
