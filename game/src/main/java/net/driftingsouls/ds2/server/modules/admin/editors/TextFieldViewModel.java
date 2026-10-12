package net.driftingsouls.ds2.server.modules.admin.editors;

public class TextFieldViewModel extends InputViewModel
{
	public boolean disabled;
	public String value;
	public AutoNumericViewModel autoNumeric;

	public TextFieldViewModel(String name)
	{
		super("textfield", name);
	}
}
