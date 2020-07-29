package com.alphawallet.app.ui.widget.adapter;

/**
 * Created by JB on 7/07/2020.
 */

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.Event;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.ActivityDataInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnEventClickListener;
import com.alphawallet.app.ui.widget.OnTransactionClickListener;
import com.alphawallet.app.ui.widget.entity.DateSortedItem;
import com.alphawallet.app.ui.widget.entity.EventSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TimestampSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.ui.widget.entity.TransactionSortedItem;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.EventHolder;
import com.alphawallet.app.ui.widget.holder.TransactionDateHolder;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    private int layoutResId = -1;

    private final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem left, SortedItem right)
        {
            return left.compare(right);
        }

        @Override
        public boolean areContentsTheSame(SortedItem oldItem, SortedItem newItem) {
            return oldItem.areContentsTheSame(newItem);
        }

        @Override
        public boolean areItemsTheSame(SortedItem left, SortedItem right) {
            return left.areItemsTheSame(right);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    });

    private final OnTransactionClickListener onTransactionClickListener;
    private final OnEventClickListener onEventClickListener;

    private Wallet wallet;
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final ActivityDataInteract dataInteract;
    private final AssetDefinitionService assetService;
    private long fetchData = 0;
    private final Handler handler = new Handler();
    private int itemLimit = 0;

    public ActivityAdapter(OnTransactionClickListener onTransactionClickListener, OnEventClickListener eventClickListener, TokensService service,
                           FetchTransactionsInteract fetchTransactionsInteract, AssetDefinitionService svs, ActivityDataInteract dataInteract) {
        this.onTransactionClickListener = onTransactionClickListener;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.dataInteract = dataInteract;
        this.assetService = svs;
        this.onEventClickListener = eventClickListener;
        tokensService = service;
        setHasStableIds(true);
    }

    public ActivityAdapter(OnTransactionClickListener onTransactionClickListener, OnEventClickListener eventClickListener, TokensService service,
                               FetchTransactionsInteract fetchTransactionsInteract, AssetDefinitionService svs, int layoutResId) {
        this.onTransactionClickListener = onTransactionClickListener;
        this.onEventClickListener = eventClickListener;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        tokensService = service;
        setHasStableIds(true);
        this.layoutResId = layoutResId;
        this.dataInteract = null;
        this.assetService = svs;
    }

    @Override
    public BinderViewHolder<?> onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TransactionHolder.VIEW_TYPE:
                TransactionHolder transactionHolder
                        = new TransactionHolder(getTxLayoutId(), parent, tokensService, fetchTransactionsInteract);
                transactionHolder.setOnTransactionClickListener(onTransactionClickListener);
                holder = transactionHolder;
                break;
            case EventHolder.VIEW_TYPE:
                holder = new EventHolder(R.layout.item_event, parent, tokensService, fetchTransactionsInteract,
                        assetService, onEventClickListener);
                break;
            case TransactionDateHolder.VIEW_TYPE:
                holder = new TransactionDateHolder(R.layout.item_transactions_date_head, parent);
                break;
        }
        return holder;
    }

    private int getTxLayoutId()
    {
        if (this.layoutResId != -1) {
            return R.layout.item_recent_transaction;
        } else {
            return R.layout.item_transaction;
        }
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        Bundle addition = new Bundle();
        addition.putString(TransactionHolder.DEFAULT_ADDRESS_ADDITIONAL, wallet.address);
        //addition.putString(TransactionHolder.DEFAULT_SYMBOL_ADDITIONAL, network.symbol);
        holder.bind(items.get(position).value, addition);
        if (dataInteract != null && System.currentTimeMillis() > fetchData && position > items.size() - 100)
        {
            fetchData = System.currentTimeMillis() + 10*DateUtils.SECOND_IN_MILLIS;
            handler.post(checkData);
        }
    }

    private void fetchData(long earliestDate)
    {
        if (dataInteract != null) dataInteract.fetchMoreData(earliestDate);
    }

    private Runnable checkData = () -> {
        //get final position time
        SortedItem item = items.get(items.size() - 1);
        long earliestDate = 0;
        if (item instanceof TransactionSortedItem)
        {
            earliestDate = ((TransactionSortedItem)item).value.timeStamp;
        }
        else if (item instanceof DateSortedItem)
        {
            earliestDate = ((DateSortedItem)item).value.getTime();
        }
        else if (item instanceof EventSortedItem)
        {
            earliestDate = ((EventSortedItem)item).value.timeStamp;
        }

        fetchData(earliestDate);
    };

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    public void setDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        Object obj = items.get(position);
        if (obj instanceof TransactionSortedItem) {
            TransactionMeta tm = ((TransactionSortedItem)obj).value;
            return tm.getUID();
        }
        else if (obj instanceof EventSortedItem) {
            EventMeta em = ((EventSortedItem)obj).value;
            return em.getUID();
        }
        else {
            return position;
        }
    }

    public void updateActivityItems(ActivityMeta[] activityItems)
    {
        if (activityItems.length == 0) return ;

        items.beginBatchedUpdates();
        for (ActivityMeta item : activityItems)
        {
            if (item instanceof TransactionMeta)
            {
                TransactionSortedItem sortedItem = new TransactionSortedItem(TransactionHolder.VIEW_TYPE, (TransactionMeta)item, TimestampSortedItem.DESC);
                items.add(sortedItem);
            }
            else if (item instanceof EventMeta)
            {
                EventSortedItem sortedItem = new EventSortedItem(EventHolder.VIEW_TYPE, (EventMeta)item, TimestampSortedItem.DESC);
                items.add(sortedItem);
            }
            items.add(DateSortedItem.round(item.timeStamp));
        }

        applyItemLimit();

        items.endBatchedUpdates();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    private void applyItemLimit()
    {
        if (itemLimit == 0) return;
        int count = 0;
        List<SortedItem> removalObjects = new ArrayList<>();

        //items should be sorted in date order already
        for (int p = 0; p < items.size(); p++)
        {
            if (items.get(p).value instanceof ActivityMeta)
            {
                count++;
            }

            if (count > itemLimit)
            {
                removalObjects.add(items.get(p));
            }
        }

        for (SortedItem sortedItem : removalObjects)
        {
            items.remove(sortedItem);
        }
    }

    public void updateItems(List<ContractLocator> tokenContracts)
    {
        //find items ssd
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).viewType == TransactionHolder.VIEW_TYPE
                && items.get(i).value instanceof TransactionMeta)
            {
                TransactionMeta tm = (TransactionMeta)items.get(i).value;
                if (tm.contractAddress != null && hasMatchingContract(tokenContracts, tm.contractAddress.toLowerCase()))
                {
                    notifyItemChanged(i);
                }
            }
        }
    }

    private TransactionMeta findMetaInAdapter(TransactionMeta meta)
    {
        //find items ssd
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).viewType == TransactionHolder.VIEW_TYPE
                    && items.get(i).value instanceof TransactionMeta)
            {
                TransactionMeta tm = (TransactionMeta)items.get(i).value;
                return tm;
            }
        }

        return null;
    }

    private boolean hasMatchingContract(List<ContractLocator> tokenContracts, String itemContractAddr)
    {
        for (ContractLocator cl : tokenContracts)
        {
            if (cl.address.equalsIgnoreCase(itemContractAddr))
            {
                return true;
            }
        }

        return false;
    }

    public void setItemLimit(int historyCount)
    {
        itemLimit = historyCount;
    }
}

