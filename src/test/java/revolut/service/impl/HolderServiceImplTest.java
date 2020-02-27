package revolut.service.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import revolut.model.Account;
import revolut.model.Holder;
import revolut.service.HolderService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HolderServiceImplTest {
    private static HolderService holderService;

    @BeforeAll
    static void setUp() {
        holderService = new HolderServiceImpl();
    }

    @Test
    void getHolders() {
        Map<Long, Holder> holders = holderService.getHolders();
        assertFalse(holders.isEmpty());
    }

    @Test
    void getHolderById() {
        Holder holder = holderService.getHolderById(1L);
        assertNotNull(holder);
    }

    @Test
    void getAccountById() {
        Account account = holderService.getAccountById(1L);
        assertNotNull(account);
    }
}