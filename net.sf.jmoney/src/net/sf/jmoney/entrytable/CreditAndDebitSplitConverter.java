package net.sf.jmoney.entrytable;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.IBidiWithStatusConverter;
import org.eclipse.core.internal.databinding.provisional.bind.IValueWithStatus;
import org.eclipse.core.internal.databinding.provisional.bind.ValueWithStatus;
import org.eclipse.core.runtime.CoreException;

import net.sf.jmoney.fields.IAmountFormatter;

public class CreditAndDebitSplitConverter implements IBidiWithStatusConverter<Long, String> {

	private IAmountFormatter formatter;

	private boolean isDebit;

	private IObservableValue<Long> modelObservable;

	/**
	 * 
	 * @param formatter
	 * 			formats and parses amounts in a way appropriate for the commodity
	 * @param isDebit <code>true</code> if debit column, <code>false</code> if credit column 
	 * @param modelObservable the model observable used in the binding, this being a hack because the
	 * 			converter needs to know the previous model value when converting from target to model
	 */
	public CreditAndDebitSplitConverter(IAmountFormatter formatter, boolean isDebit, IObservableValue<Long> modelObservable) {
		this.formatter = formatter;
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
					? formatter.format(-amount)
							: ""; //$NON-NLS-1$
		} else {
			// Credit column
			return amount > 0
					? formatter.format(amount)
							: ""; //$NON-NLS-1$
		}
	}

	@Override
	public IValueWithStatus<Long> targetToModel(String fromObject) {
		String amountString = fromObject;

		try {
			long amount = formatter.parse(amountString);

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

			return ValueWithStatus.ok(newEntryAmount);
		} catch (CoreException e) {
			// Should we just pass the status directly???
			return ValueWithStatus.error(e.getLocalizedMessage());
		}
	}
}