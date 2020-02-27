package revolut.service;

import revolut.model.Account;
import revolut.model.Holder;

import java.util.Map;

public interface HolderService {
    Map<Long, Holder> getHolders();

    Holder getHolderById(Long id);

    Account getAccountById(Long id);
}
