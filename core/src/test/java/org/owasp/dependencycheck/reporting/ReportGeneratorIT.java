/*
 * This file is part of dependency-check-core.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.reporting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.owasp.dependencycheck.BaseDBTestCase;
import org.owasp.dependencycheck.BaseTest;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.exception.ReportException;
import org.owasp.dependencycheck.utils.InvalidSettingException;
import org.owasp.dependencycheck.utils.Settings;
import org.xml.sax.SAXException;
import static org.junit.Assert.fail;
import org.owasp.dependencycheck.data.update.exception.UpdateException;
import org.owasp.dependencycheck.utils.DownloadFailedException;

/**
 *
 * @author Jeremy Long
 */
public class ReportGeneratorIT extends BaseDBTestCase {

    /**
     * Generates an XML report containing known vulnerabilities and realistic
     * data and validates the generated XML document against the XSD.
     */
    @Test
    public void testGenerateReport() {
        try {
            File f = new File("target/test-reports");
            if (!f.exists()) {
                f.mkdir();
            }
            File writeTo = new File("target/test-reports/Report.xml");
            File writeJsonTo = new File("target/test-reports/Report.json");
            File writeHtmlTo = new File("target/test-reports/Report.html");
            File suppressionFile = BaseTest.getResourceAsFile(this, "incorrectSuppressions.xml");
            Settings settings = getSettings();
            settings.setString(Settings.KEYS.SUPPRESSION_FILE, suppressionFile.getAbsolutePath());
            settings.setBoolean(Settings.KEYS.AUTO_UPDATE, false);
            settings.setBoolean(Settings.KEYS.ANALYZER_RETIREJS_ENABLED, false);
            settings.setBoolean(Settings.KEYS.ANALYZER_NODE_AUDIT_ENABLED, false);
            settings.setBoolean(Settings.KEYS.ANALYZER_NODE_PACKAGE_ENABLED, false);
            settings.setBoolean(Settings.KEYS.ANALYZER_CENTRAL_ENABLED, false);

            //File struts = new File(this.getClass().getClassLoader().getResource("struts2-core-2.1.2.jar").getPath());
            File struts = BaseTest.getResourceAsFile(this, "struts2-core-2.1.2.jar");
            //File axis = new File(this.getClass().getClassLoader().getResource("axis2-adb-1.4.1.jar").getPath());
            File axis = BaseTest.getResourceAsFile(this, "axis2-adb-1.4.1.jar");
            //File jetty = new File(this.getClass().getClassLoader().getResource("org.mortbay.jetty.jar").getPath());
            File jetty = BaseTest.getResourceAsFile(this, "org.mortbay.jetty.jar");

            File nodeTest = BaseTest.getResourceAsFile(this, "nodejs");

            try (Engine engine = new Engine(settings)) {
                engine.scan(struts);
                engine.scan(axis);
                engine.scan(jetty);
                engine.scan(nodeTest);
                engine.analyzeDependencies();
                ExceptionCollection exceptions = new ExceptionCollection();
                exceptions.addException(new DownloadFailedException("test exception 1"));
                DownloadFailedException sub = new DownloadFailedException("test cause exception - nested");
                DownloadFailedException inner = new DownloadFailedException("Unable to download test file", sub.fillInStackTrace());
                UpdateException ex = new UpdateException("Test Exception 2", inner.fillInStackTrace());
                exceptions.addException(ex);
                engine.writeReports("Test Report", "org.owasp", "dependency-check-core", "1.4.8", writeTo, "XML", exceptions);
                engine.writeReports("Test Report", "org.owasp", "dependency-check-core", "1.4.8", writeJsonTo, "JSON", exceptions);
                engine.writeReports("Test Report", "org.owasp", "dependency-check-core", "1.4.8", writeHtmlTo, "HTML", exceptions);
            }
            InputStream xsdStream = ReportGenerator.class.getClassLoader().getResourceAsStream("schema/dependency-check.2.2.xsd");
            StreamSource xsdSource = new StreamSource(xsdStream);
            StreamSource xmlSource = new StreamSource(writeTo);
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(xsdSource);
            Validator validator = schema.newValidator();
            validator.validate(xmlSource);
        } catch (InvalidSettingException ex) {
            fail(ex.getMessage());
        } catch (DatabaseException | ExceptionCollection | ReportException | SAXException | IOException ex) {
            fail(ex.getMessage());
        }
    }
}
