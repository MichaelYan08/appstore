package org.literacyapp.appstore.dao.converter;

import android.util.Log;

import org.greenrobot.greendao.converter.PropertyConverter;
import org.literacyapp.model.enums.admin.ApplicationStatus;

public class ApplicationStatusConverter implements PropertyConverter<ApplicationStatus, String> {

    @Override
    public ApplicationStatus convertToEntityProperty(String databaseValue) {
        Log.i(getClass().getName(), "convertToEntityProperty");

        ApplicationStatus entityProperty = ApplicationStatus.valueOf(databaseValue);
        Log.i(getClass().getName(), "entityProperty: " + entityProperty);
        return entityProperty;
    }

    @Override
    public String convertToDatabaseValue(ApplicationStatus entityProperty) {
        Log.i(getClass().getName(), "convertToDatabaseValue");

        String databaseValue = entityProperty.toString();
        Log.i(getClass().getName(), "databaseValue: " + databaseValue);
        return databaseValue;
    }
}