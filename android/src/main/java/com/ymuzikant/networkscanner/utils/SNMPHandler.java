package com.ymuzikant.networkscanner.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.net.InetAddress;

/**
 * Created by Yaron Muzikant on 30-Mar-17.
 */

public class SNMPHandler {
    private static final String TAG = "SNMPHandler";
    private static Snmp snmp = null;

    private final static String SNMP_OID_DEVICE_SYS_DESCR = "1.3.6.1.2.1.1.1.0";
    private final static String SNMP_OID_DEVICE_SYS_OBJECT_ID = "1.3.6.1.2.1.1.2.0";
    private final static String SNMP_OID_DEVICE_SYS_NAME = "1.3.6.1.2.1.1.5.0";
    private final static String SNMP_OID_DEVICE_SYS_LOCATION = "1.3.6.1.2.1.1.6.0";

    public JSONObject getSNMPInfo(String ip) {
        try {
            init();

            UdpAddress targetAddress = new UdpAddress(InetAddress.getByName(ip), 161);
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(targetAddress);
            target.setRetries(1);
            target.setTimeout(1000);
            target.setVersion(SnmpConstants.version1);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(SNMP_OID_DEVICE_SYS_DESCR)));
            pdu.add(new VariableBinding(new OID(SNMP_OID_DEVICE_SYS_OBJECT_ID)));
            pdu.add(new VariableBinding(new OID(SNMP_OID_DEVICE_SYS_NAME)));
            pdu.add(new VariableBinding(new OID(SNMP_OID_DEVICE_SYS_LOCATION)));
            pdu.setType(PDU.GET);

            ResponseEvent snmpResponse = snmp.send(pdu, target, null);

            if (snmpResponse.getResponse() != null) {
                Log.d(TAG, "snmp info for ip = [" + ip + "], info = [" + snmpResponse.getResponse() + "]");
                return snmpResponseToJSON(snmpResponse);
            }
        } catch (Exception e) {
            Log.e(TAG, "getSNMPInfo: Error getting SNMP info for " + ip, e);
        }
        return null;
    }

    private void init() {
        synchronized (SNMPHandler.class) {
            if (snmp == null) {
                try {
                    TransportMapping transport = new DefaultUdpTransportMapping();
                    snmp = new Snmp(transport);
                    transport.listen();
                } catch (Exception e) {
                    Log.e(TAG, "init SNMP failed", e);
                }
            }
        }
    }

    private JSONObject snmpResponseToJSON(ResponseEvent snmpResponse) {
        JSONObject snmpInfo = new JSONObject();

        if (snmpResponse.getResponse() != null) {
            for (int i = 0; i < snmpResponse.getRequest().size(); i++) {
                VariableBinding variableBinding = snmpResponse.getResponse().get(i);
                try {
                    snmpInfo.put(
                            getSNMPPropertyName(variableBinding.getOid()),
                            variableBinding.toValueString());
                } catch (JSONException e) {
                    Log.e(TAG, "Error building snmp info JSON", e);
                }
            }
        }

        return snmpInfo;
    }

    private String getSNMPPropertyName(@NonNull OID oid) {
        switch (oid.toString()) {
            case SNMP_OID_DEVICE_SYS_DESCR:
                return "sysDescr";
            case SNMP_OID_DEVICE_SYS_LOCATION:
                return "sysLocation";
            case SNMP_OID_DEVICE_SYS_NAME:
                return "sysName";
            case SNMP_OID_DEVICE_SYS_OBJECT_ID:
                return "sysObjectID";
            default:
                return oid.toString().replaceAll("\\.", "_");
        }
    }
}