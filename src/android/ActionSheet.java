package nl.xservices.plugins.actionsheet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * @author Original excellent PR by: Brill Pappin
 * @author Mantido pela MBA.
 */
public class ActionSheet extends CordovaPlugin {

  private AlertDialog dialog;
  private HashMap<String,Boolean> buttonMap = new HashMap<String,Boolean>();

  public ActionSheet() {
    super();
  }

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

    if ("show".equals(action)) {
      JSONObject options = args.optJSONObject(0);

      String title = options.optString("title");
      int theme = options.optInt("androidTheme", 1);
      JSONArray buttons = options.optJSONArray("buttonLabels");

      boolean androidEnableCancelButton = options.optBoolean("androidEnableCancelButton", false);

      String addCancelButtonWithLabel = options.optString("addCancelButtonWithLabel");
      String addDestructiveButtonWithLabel = options.optString("addDestructiveButtonWithLabel");

      this.show(title, buttons, addCancelButtonWithLabel,
          androidEnableCancelButton, addDestructiveButtonWithLabel,
          theme,
          callbackContext);
      // need to return as this call is async.
      return true;
    } else if ("hide".equals(action)) {
      if (dialog != null && dialog.isShowing()) {
        dialog.dismiss();
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, -1));
      }
      return true;
    }
    return false;
  }

  public synchronized void show(final String title,
                                final JSONArray buttonLabels,
                                final String addCancelButtonWithLabel,
                                final boolean androidEnableCancelButton,
                                final String addDestructiveButtonWithLabel,
                                final int theme,
                                final CallbackContext callbackContext) {

    final CordovaInterface cordova = this.cordova;

    Runnable runnable = new Runnable() {
      public void run() {

        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          builder = new AlertDialog.Builder(cordova.getActivity(), theme);
        } else {
          builder = new AlertDialog.Builder(cordova.getActivity());
        }

        builder
            .setTitle(title)
            .setCancelable(true);

        if (androidEnableCancelButton && !TextUtils.isEmpty(addCancelButtonWithLabel)) {
          builder.setNegativeButton(addCancelButtonWithLabel,
              new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  dialog.cancel();
                }
              });
        }

        final String[] buttons = getStringArray(
            buttonLabels,
            (TextUtils.isEmpty(addDestructiveButtonWithLabel) ? null
                : addDestructiveButtonWithLabel));

        final ListAdapter adapter = new ArrayAdapter(
                cordova.getActivity(), android.R.layout.file, buttons) {
          ViewHolder button;
          class ViewHolder {
            Button button;
          }
            public View getView(int position, View convertView,
                                ViewGroup parent) {
              final LayoutInflater inflater = (LayoutInflater) cordova.getActivity().getApplicationContext()
                      .getSystemService(
                              Context.LAYOUT_INFLATER_SERVICE);
              final int wich = position;
              if (convertView == null) {
                convertView = inflater.inflate(
                        android.R.layout.file, null);

                button = new ViewHolder();
                button.button = (Button) convertView
                        .findViewById(android.R.layout.id.button);
                convertView.setTag(button);
              } else {
                // view already defined, retrieve view holder
                button = (ViewHolder) convertView.getTag();
              }
              button.button.setEnabled(!buttonMap.get(buttons[position]));
              button.button.setText(buttons[position]);
              button.button.setTextSize(TypedValue.COMPLEX_UNIT_PX,28);
              button.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                  callbackContext.sendPluginResult(new PluginResult(
                          PluginResult.Status.OK, wich + 1));
                }
              });

              return convertView;
            }
          };

        builder.setAdapter(adapter, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
          }
        });

        builder.setOnCancelListener(new AlertDialog.OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            int cancelButtonIndex = buttons.length + 1;
            callbackContext.sendPluginResult(new PluginResult(
                    PluginResult.Status.OK, cancelButtonIndex));
          }
        });

        dialog = builder.create();
        dialog.show();
      }
    };
    this.cordova.getActivity().runOnUiThread(runnable);
  }

  private String[] getStringArray(JSONArray jsonArray, String... prepend) {

    List<String> btn = new ArrayList<String>();

    // Add prefix items like destructive buttons.
    for (String aPrepend : prepend) {
      if (!TextUtils.isEmpty(aPrepend)) {
        btn.add(aPrepend);
      }
    }
    Boolean disabled = false;
    Boolean hidden = false;
    String text = "";
    if (jsonArray != null) {
      for(int i=0; i<jsonArray.length(); i++) {
        try {
          JSONObject obj = jsonArray.getJSONObject(i);
          text = obj.optString("text");
          disabled = obj.optBoolean("disabled");
          hidden = obj.optBoolean("hidden");
          if (hidden) {
            continue;
          }
          btn.add(text);
        } catch (org.json.JSONException e) {
          //continuar
        }
        buttonMap.put(text,disabled);
        disabled = false;
      }
    }

    return btn.toArray(new String[btn.size()]);
  }
}
