package revolut.service.impl;

import revolut.model.Account;
import revolut.model.Holder;
import revolut.provider.DataProvider;
import revolut.service.HolderService;

import java.util.Map;

public class HolderServiceImpl implements HolderService {
    /* holders is an immutable hash map.*/
    private Map<Long, Holder> holders;

    public HolderServiceImpl() {
        holders = DataProvider.generateHolders(3);
    }

    @Override
    public Map<Long, Holder> getHolders() {
        return holders;
    }

    @Override
    public Holder getHolderById(Long id) {
        return holders.get(id);
    }

    @Override
    public Account getAccountById(Long id) {
        for (Map.Entry<Long, Holder> holder : holders.entrySet()) {
            if (holder.getValue().getAccounts().get(id) != null) {
                return holder.getValue().getAccounts().get(id);
            }
        }
        return null;
    }
}
