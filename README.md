iOS message export
==================

This small java application can export messages from a iOS backup.
Unfortunately, you have to have an unencrypted backup for reading the data.
Also attached images will be exported.

The application does:
 - reads the contact database
 - read all messages of the contact (joined by email and phone)
 - export the message to a HTML of every contact including the attached images

The iOS backup is located on OS X at

    ~/Library/Application Support/MobileSync/Backup/<phone ID>/

and at Windows (not verified)

    <user directory>\AppData\Roaming\Apple Computer\MobileSync\Backup\<phone ID>\

Usage
-----

    java -cp <lib> msgexport.MsgExportApplication backup-dir country-code area-code [export-dir]

        backup-dir   - directory of the unportected backup
        country-code - country code for phone numbers, eg. 1 for US or 49 for germany
        area-code    - area code for phone numbers
        export-dir   - directory for the export files (optional)

The ``country-code`` and ``area-code`` are needed to rectify the telephone numbers from the contact database.

Issues
======

Rotated images are displayed only in Firefox correctly.

License
=======

MIT License. Copyright 2016 Hansjörg Oppermann