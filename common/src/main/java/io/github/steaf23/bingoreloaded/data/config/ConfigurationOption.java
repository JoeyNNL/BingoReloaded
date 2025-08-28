package io.github.steaf23.bingoreloaded.data.config;

import io.github.steaf23.bingoreloaded.lib.data.core.DataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ConfigurationOption<Data>
{
    public enum EditUpdateTime {
        IMMEDIATE,
        AFTER_GAME,
        AFTER_SERVER_RESTART,
        AFTER_SESSION,
        IMPOSSIBLE,
    }

    public static class StringList extends ArrayList<String>
    {
        public StringList(List<String> in)
        {
            this.addAll(in);
        }
    }

    private final String configName;
    private EditUpdateTime editUpdate = EditUpdateTime.IMMEDIATE;
    private boolean locked = false;
	private boolean nullable = false;

    public ConfigurationOption(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public EditUpdateTime getEditUpdateTime() {
        return editUpdate;
    }

    public ConfigurationOption<Data> withEditUpdate(EditUpdateTime editUpdate) {
        this.editUpdate = editUpdate;
        return this;
    }

    public ConfigurationOption<Data> lock() {
        locked = true;
        return this;
    }

	public ConfigurationOption<Data> makeNullable() {
		nullable = true;
		return this;
	}

    public boolean canBeEdited() {
        return getEditUpdateTime() != EditUpdateTime.IMPOSSIBLE && !isLocked();
    }

    public boolean isLocked() {
        return locked;
    }

	public boolean isNullable() {
		return nullable;
	}

    abstract public Optional<Data> fromString(String value);

    abstract public void toDataStorage(DataStorage storage, @Nullable Data value);
}
