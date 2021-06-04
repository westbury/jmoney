package net.sf.jmoney.stocks.types;

import java.util.HashSet;
import java.util.Set;

import net.sf.jmoney.stocks.pages.IEntryType;

public enum TransactionType {
		Buy("stocks.buy", BuyOrSellEntryType.values()),
		Sell("stocks.sell", BuyOrSellEntryType.values()),
		Dividend("stocks.dividend", DividendEntryType.values()),
		Takeover("stocks.takeover", TakeoverEntryType.values()),
		Other(null, new IEntryType[0]);   // all other transaction types use this.  Should it just be a null type?

		private String id;
		private IEntryType[] entryTypes;
		private Set<String> compulsoryEntryTypes;
		
		TransactionType(String id, IEntryType[] entryTypes) {
			this.id = id;
			this.entryTypes = entryTypes;

//			this.entryTypes = new HashSet<>();
//			for (IEntryType entryType : entryTypes) {
//				this.entryTypes.add(entryType.getId());
//			}

			this.compulsoryEntryTypes = new HashSet<>();
			for (IEntryType entryType : entryTypes) {
				if (entryType.isCompulsory()) {
					this.compulsoryEntryTypes.add(entryType.getId());
				}
			}
		}

		public IEntryType[] getEntryTypes() {
			return this.entryTypes;
		}

		public Set<String> getCompulsoryEntryTypes() {
			return this.compulsoryEntryTypes;
		}

		/**
		 * This is the first part of each triple in the entry type.
		 * 
		 * @return
		 */
		public String getId() {
			return id;
		}
	}