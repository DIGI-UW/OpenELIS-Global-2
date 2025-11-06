/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.common.services;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
public class AddressService {

    public static String getAddresslineLabel1() {
        return ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.Addressline1label);
    }

    public static String getAddresslineLabel2() {
        return ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.Addressline2label);
    }

    public static String getAddresslineLabel3() {
        return ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.Addressline3label);
    }

    public static String getGeographicUnitLabel1() {
        return ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.GeographicUnit1Label);
    }

    public static String getGeographicUnitLabel2() {
        return ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.GeographicUnit2Label);
    }
}
