package com.example.bank.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyTargetNotFound() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-NONEXISTENT\"," +
                "\"amount\":500.00," +
                "\"description\":\"Test payment\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Target account not found"));
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneySourceOwnershipFailure() throws Exception {
        // ACC-987654 belongs to Bob, not Alice
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-987654\"," +
                "\"targetAccountNumber\":\"ACC-123456\"," +
                "\"amount\":500.00," +
                "\"description\":\"Test parameter tampering\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden: You do not own the source account."));
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyNegativeAmountBlocked() throws Exception {
        // Enforced by TransferRequest DTO Validation
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":-100.00," +
                "\"description\":\"Test negative amount\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneySuccess() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":100.00," +
                "\"description\":\"Secure test transfer\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer completed successfully"));
     }



    @Test
    @WithMockUser(username = "alice")
    public void testGetTransactionHistoryIDORBlocked() throws Exception {
        // ACC-987654 belongs to Bob, not Alice
        mockMvc.perform(get("/api/transactions/history")
                        .param("accountNumber", "ACC-987654"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied."));
    }

    @Test
    @WithMockUser(username = "alice")
    public void testGetTransactionHistorySuccess() throws Exception {
        mockMvc.perform(get("/api/transactions/history")
                        .param("accountNumber", "ACC-123456"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneySameAccountBlocked() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-123456\"," +
                "\"amount\":100.00," +
                "\"description\":\"Self transfer\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot transfer to the same account."));
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyTinyAmountBlocked() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":0.005," +
                "\"description\":\"Tiny amount\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyStringAmountBlocked() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":\"10.00\"," +
                "\"description\":\"String amount\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyTooManyDecimalsBlocked() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":10.001," +
                "\"description\":\"Too many decimals\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyLimitExceededBlocked() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":99999999.00," +
                "\"description\":\"Above limit\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyNaNBlocked() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":NaN," +
                "\"description\":\"NaN amount\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    public void testTransferMoneyInfinityBlocked() throws Exception {
        String body = "{" +
                "\"sourceAccountNumber\":\"ACC-123456\"," +
                "\"targetAccountNumber\":\"ACC-987654\"," +
                "\"amount\":Infinity," +
                "\"description\":\"Infinity amount\"" +
                "}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
