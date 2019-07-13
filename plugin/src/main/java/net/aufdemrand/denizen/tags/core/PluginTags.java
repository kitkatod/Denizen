package net.aufdemrand.denizen.tags.core;

import net.aufdemrand.denizen.objects.dPlugin;
import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class PluginTags {

    public PluginTags() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                pluginTags(event);
            }
        }, "plugin");
    }

    public void pluginTags(ReplaceableTagEvent event) {

        if (!event.matches("plugin") || event.replaced()) {
            return;
        }

        dPlugin plugin = null;

        if (event.hasNameContext()) {
            plugin = dPlugin.valueOf(event.getNameContext(), event.getAttributes().context);
        }

        if (plugin == null) {
            return;
        }

        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(plugin, attribute.fulfill(1)));

    }
}
