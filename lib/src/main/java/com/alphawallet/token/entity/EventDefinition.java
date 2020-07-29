package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListSelectionModel;

/**
 * Created by JB on 21/03/2020.
 */
public class EventDefinition
{
    public ContractInfo contract;
    public String attributeName; //TransactionResult: method
    public NamedType type;
    public String filter;
    public String select;
    public BigInteger readBlock;
    public Attribute parentAttribute;
    public String activityName = null;

    public String getFilterTopicValue()
    {
        // This regex splits up the "filterArgName=${filterValue}" directive and gets the 'filterValue'
        Matcher m = Pattern.compile("\\$\\{([^}]+)\\}").matcher(filter);
        return (m.find() && m.groupCount() >= 1) ? m.group(1) : null;
    }

    public String getFilterTopicIndex()
    {
        // Get the filter name from the directive and strip whitespace
        String[] item = filter.split("=");
        return item[0].replaceAll("\\s+", "");
    }

    public int getTopicIndex(String filterTopic)
    {
        if (type == null || filterTopic == null) return -1;
        return type.getTopicIndex(filterTopic);
    }

    public int getSelectIndex(boolean indexed)
    {
        int index = 0;
        boolean found = false;
        for (String label : type.getArgNames(indexed))
        {
            if (label.equals(select))
            {
                found = true;
                break;
            }
            else
            {
                index++;
            }
        }

        return found ? index : -1;
    }

    public int getEventChainId()
    {
        if (parentAttribute != null)
        {
            return parentAttribute.originContract.addresses.keySet().iterator().next();
        }
        else
        {
            return contract.addresses.keySet().iterator().next();
        }
    }

    public String getEventContractAddress()
    {
        int chainId = getEventChainId();
        String contractAddress;
        if (parentAttribute != null)
        {
            contractAddress = parentAttribute.originContract.addresses.get(chainId).get(0);
        }
        else
        {
            contractAddress = contract.addresses.get(chainId).get(0);
        }

        return contractAddress;
    }

    public int getNonIndexedIndex(String name)
    {
        if (type == null || name == null) return -1;
        return type.getNonIndexedIndex(name);
    }

    public boolean equals(EventDefinition ev)
    {
        if (contract.getfirstChainId() == ev.contract.getfirstChainId() && contract.getFirstAddress().equalsIgnoreCase(ev.contract.getFirstAddress()) &&
                filter.equals(ev.filter) && type.name.equals(ev.type.name) && (
                (activityName != null && ev.activityName != null && activityName.equals(ev.activityName)) ||
                (attributeName != null && ev.attributeName != null && attributeName.equals(ev.attributeName))) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
