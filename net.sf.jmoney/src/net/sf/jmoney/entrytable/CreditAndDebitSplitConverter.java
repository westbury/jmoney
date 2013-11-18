package net.sf.jmoney.entrytable;

import net.sf.jmoney.model2.Commodity;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiConverter;
import org.eclipse.core.runtime.CoreException;

public class CreditAndDebitSplitConverter implements IBidiConverter<Long, String> {

private Commodity commodityForFormatting;

private boolean isDebit;

private IObservableValue<Long> modelObservable;

/**
 * 
 * @param commodityForFormatting
 * @param isDebit <code>true</code> if debit column, <code>false</code> if credit column 
 * @param modelObservable the model observable used in the binding, this being a hack because the
 * 			converter needs to know the previous model value when converting from target to model
 */
public CreditAndDebitSplitConverter(Commodity commodityForFormatting, boolean isDebit, IObservableValue<Long> modelObservable) {
	this.commodityForFormatting = commodityForFormatting;
	this.isDebit = isDebit;
	this.modelObservable = modelObservable;
}

@Override
public String modelToTarget(Long amount) {
	if (amount == null) {
		// Although amounts are never null in an entry,
		// the amount may be null here if the master object is null
		return ""; //$NON-NLS-1$
	} else if (isDebit) {
		// Debit column
		return amount < 0
				? commodityForFormatting.format(-amount)
						: ""; //$NON-NLS-1$
	} else {
		// Credit column
		return amount > 0
				? commodityForFormatting.format(amount)
						: ""; //$NON-NLS-1$
	}
}

@Override
public Long targetToModel(String fromObject)
		throws CoreException {
	String amountString = fromObject;
	long amount = commodityForFormatting.parse(amountString);

	long previousEntryAmount = modelObservable.getValue();
	long newEntryAmount;

	if (isDebit) {
		if (amount != 0) {
			newEntryAmount = -amount;
		} else {
			if (previousEntryAmount < 0) {
				newEntryAmount  = 0;
			} else {
				newEntryAmount = previousEntryAmount;
			}
		}
	} else {
		if (amount != 0) {
			newEntryAmount = amount;
		} else {
			if (previousEntryAmount > 0) {
				newEntryAmount  = 0;
			} else {
				newEntryAmount = previousEntryAmount;
			}
		}
	}

	return newEntryAmount;
}
}