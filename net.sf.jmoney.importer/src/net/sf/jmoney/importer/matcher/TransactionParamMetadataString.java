package net.sf.jmoney.importer.matcher;

import java.text.MessageFormat;

import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.MemoPatternInfo;

import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.provisional.bind.Bind;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.internal.databinding.provisional.swt.UpdatingComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class TransactionParamMetadataString extends TransactionParamMetadata {

	public TransactionParamMetadataString(String id, String name) {
		super(id, name);
	}
	
	@Override
	public Control createControl(UpdatingComposite parent, final IObservableValue<MemoPattern> memoPattern, final IObservableValue<String[]> args) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		
		Text textbox = new Text(composite, SWT.NONE);

		final Label label2 = new Label(composite, SWT.NONE);

		final IObservableValue<String> parameterValueProperty = MemoPatternInfo.getParameterValueProperty(getId()).observeDetail(memoPattern);
		Bind.twoWay(parameterValueProperty).to(WidgetProperties.textText(new int[] { SWT.Modify }), textbox);

		/*
		 * We can't use computed values here because we are in a tracked getter, and
		 * inside that with tracking turned off.  Databinding really should support nesting this,
		 * but until then, we run this asynchronously.
		 */
		Display.getCurrent().asyncExec(new Runnable() {

			@Override
			public void run() {
				IObservableValue<String> value = new ComputedValue<String>() {
					@Override
					protected String calculate() {
						if (args.getValue() != null && parameterValueProperty.getValue() != null) {
							try {
								return new java.text.MessageFormat(
										parameterValueProperty.getValue(),
										java.util.Locale.US)
								.format(args.getValue());
							} catch (IllegalArgumentException e) {
								return e.getMessage();
							}
						} else {
							return "";
						}
					}
				};
				Bind.oneWay(value).to(WidgetProperties.textLabel(), label2);
			}
		});

		return composite;
	}

	public String obtainValue(MemoPattern pattern, Object[] args) {
		return MessageFormat.format(pattern.getParameterValue(id), args);
	}

	@Override
	public String getResolvedValueAsString(MemoPattern pattern, Object[] args) {
		return obtainValue(pattern, args);
	}

}
