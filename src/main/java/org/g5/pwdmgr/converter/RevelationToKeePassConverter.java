package org.g5.pwdmgr.converter;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;
import org.apache.commons.lang3.StringUtils;
import org.g5.util.Streams;
import org.g5.util.xml.stream.XmlSpliterator;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * I convert Revelation data, exported as XML, to KeePass 1.0 XML format. The
 * generated file can then be imported into KeePass (even version 2.x).
 * 
 * Since not all Revelation fields map onto KeePass entry fields, fields that
 * can not be naturally mapped, will be stored as <i>name = value</i> pairs in the
 * notes field. These will be appended to what notes may already be present in the
 * entry.
 * 
 * KeePass 2 has more extensive custom field and attachment support. But unfortunately
 * I can not yet generate a KeePass 2 XML format. For this reason, the name-value pairs
 * transcribed above may have to be manually moved over to KeePass attributes.
 * 
 * @author gerard.fernandes@gmail.com
 *
 */
public class RevelationToKeePassConverter {
	
	private static final Logger log = LoggerFactory.getLogger(RevelationToKeePassConverter.class);
	
	public static void main(String[] args) throws Exception {
		long start = System.nanoTime();
		ArgumentParser argumentParser = argParser();
		try {
			Namespace cli = argumentParser.parseArgs(args);
			File revelationFile = cli.get("revelationFile");
			Element kpRoot = new Element("pwlist");
			Document kpXml = new Document(kpRoot);
			XmlSpliterator revelationDataElements = new XmlSpliterator(new StreamSource(revelationFile), "entry");
			
			List<Element> kpEntries = Streams.sequential(revelationDataElements)
										.map(new RevelationToKPConverterFn())
					.collect(Collectors.toList());
			kpRoot.addContent(kpEntries);
			StringWriter out = new StringWriter();
			new XMLOutputter(Format.getPrettyFormat()).output(kpXml, out);
			System.out.println(out.toString());
			FileWriter writer = new FileWriter(new File(revelationFile.getParentFile(), "keepass.export." + revelationFile.getName()));
			new XMLOutputter(Format.getPrettyFormat()).output(kpXml, writer);
		} catch (ArgumentParserException e) {
			argumentParser.printHelp();
			argumentParser.printUsage();
		} catch (RuntimeException e) {
			System.out.println("Failed to run conversion - error was: " + e.getMessage());
			argumentParser.printHelp();
		} finally {
			long end = System.nanoTime();
			log.info("Completed processing in {}ms", (end - start) / 1000000.00);
		}
	}

	private static ArgumentParser argParser() {
		ArgumentParser argumentParser = ArgumentParsers.newArgumentParser("RevelationToKeePassConverter", true);
		argumentParser.addArgument("-f", "--file")//switches
			.dest("revelationFile")//option lookup key - map long options onto the short option key
			.type(new InputFileArgument())//type converter
			.required(true)
			.help("the revelation XML format source file to convert to keepass 1.x format");
		return argumentParser;
	}
	
	private static Element simpleElement(String name, String text) {
		Element simpleElement = new Element(name);
		simpleElement.setText(text);
		return simpleElement;
	}
	
	private static String uuidString() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
	
	
	private static final class RevelationToKPConverterFn implements Function<Element, Element> {

		@Override
		public Element apply(Element revelationPwdEntry) {
			Element kpPwdEntry = new Element("pwentry");
			kpPwdEntry.addContent(simpleElement("group", "Revelation Import"));//revelationPwdEntry.getAttributeValue("type")));
			kpPwdEntry.addContent(simpleElement("title", revelationPwdEntry.getChildText("name")));
			kpPwdEntry.addContent(simpleElement("uuid", uuidString()));
			Element userNameKPField = simpleElement("username", "");
			String notesContent = revelationPwdEntry.getChildText("notes");
			Element notes = simpleElement("notes", isNotEmpty(notesContent)? notesContent+"\n" : "");
			kpPwdEntry.addContent(notes);
			kpPwdEntry.addContent(userNameKPField);
			for(Element fieldElement : revelationPwdEntry.getChildren("field")) {
				String revFieldId = fieldElement.getAttributeValue("id");
				if (revFieldId.endsWith("username")) {
					userNameKPField.setText(fieldElement.getText());
				} else if (revFieldId.endsWith("password")) {
					kpPwdEntry.addContent(simpleElement("password", fieldElement.getText()));
				} else if (revFieldId.endsWith("url")) {
					kpPwdEntry.addContent(simpleElement("url", fieldElement.getText()));
				} else {
					notes.addContent(new Text(revFieldId+"="+fieldElement.getText()+"\n"));
//					kpPwdEntry.addContent(simpleElement(revFieldId, fieldElement.getText()));
				}
			}
			LocalDateTime lastUpdated = LocalDateTime.ofInstant(
					new Date(Long.parseLong(revelationPwdEntry.getChildText("updated")) * 1000).toInstant(),
					ZoneId.systemDefault());
			String lastUpdatedTimestampISOFormat = lastUpdated.toString();
			String nowISOFormat = LocalDateTime.now().toString();
			kpPwdEntry.addContent(simpleElement("creationtime", lastUpdatedTimestampISOFormat));
			kpPwdEntry.addContent(simpleElement("lastmodtime", lastUpdatedTimestampISOFormat));
			kpPwdEntry.addContent(simpleElement("lastacesstime", nowISOFormat));
			Element expiry = simpleElement("expiretime", nowISOFormat);
			kpPwdEntry.addContent(expiry);

			return kpPwdEntry;
		}
		
	}
	
	private static Element kpFieldContent(String name, String value) {
		return kpFieldContent(name, value, null);
	}
	
	private static Element kpFieldContent(String name, String value, String valueAttributes) {
		Element kpField = new Element("String");
		kpField.addContent(simpleElement("Key", name));
		Element valueElement = simpleElement("Value", value);
		if(StringUtils.isNotEmpty(valueAttributes)) {
			for(String nameValuePair : valueAttributes.split(",")) {
				String attrName = StringUtils.substringBefore(nameValuePair, "=");
				String attrValue = StringUtils.substringAfter(nameValuePair, "=");
				valueElement.setAttribute(attrName, attrValue);
			}
		}
		kpField.addContent(valueElement);
		return kpField;
	}
	
	@SuppressWarnings("unused")
	private static Element kp2Entry(Element revelationPwdEntry) {
		Element kpPwdEntry = new Element("Entry");
		kpPwdEntry.addContent(simpleElement("Title", revelationPwdEntry.getChildText("name")));
		Element userNameKPField = kpFieldContent("UserName", "");
		kpPwdEntry.addContent(userNameKPField);
		for(Element fieldElement : revelationPwdEntry.getChildren("field")) {
			String revFieldId = fieldElement.getAttributeValue("id");
			if (revFieldId.endsWith("username")) {
				userNameKPField.getChild("Value").setText(fieldElement.getText());
			} else if (revFieldId.endsWith("password")) {
				kpPwdEntry.addContent(kpFieldContent("Password", fieldElement.getText(), "ProtectInMemory=True"));
			} else if (revFieldId.endsWith("url")) {
				kpPwdEntry.addContent(kpFieldContent("URL", fieldElement.getText()));
			} else if (revFieldId.endsWith("notes")) {
				kpPwdEntry.addContent(kpFieldContent("Notes", fieldElement.getText()));
			} else {
				kpPwdEntry.addContent(kpFieldContent(revFieldId, fieldElement.getText()));
			}
		}
		LocalDateTime lastUpdated = LocalDateTime.ofInstant(
				new Date(Long.parseLong(revelationPwdEntry.getChildText("updated")) * 1000).toInstant(),
				ZoneId.systemDefault());
		String lastUpdatedTimestampISOFormat = lastUpdated.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
		String nowISOFormat = LocalDateTime.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
		Element timesElement = new Element("Times");
		timesElement.addContent(simpleElement("CreationTime", lastUpdatedTimestampISOFormat));
		timesElement.addContent(simpleElement("LastModificationTime", lastUpdatedTimestampISOFormat));
		timesElement.addContent(simpleElement("LastAccessTime", nowISOFormat));
		timesElement.addContent(simpleElement("ExpiryTime", nowISOFormat));
		timesElement.addContent(simpleElement("Expires", "false"));
		timesElement.addContent(simpleElement("UsageCount", "1"));
		kpPwdEntry.addContent(timesElement);
		return kpPwdEntry;
	}
	
	private static final class InputFileArgument implements ArgumentType<File> {

		@Override
		public File convert(ArgumentParser parser, Argument arg, String value) {
			if (StringUtils.isNotEmpty(value)) {
				File inputFile = new File(value);
				if (inputFile.exists()) {
					log.info("Found file on file-system: {}.", value);
					return inputFile;
				} else {
					log.warn("Could not find arg[{}] value - file {} - probably not a file-path. Searching classpath...", arg.textualName(), value);
					URL inputFileUrl = getClass().getResource(value);
					if (Objects.nonNull(inputFileUrl)) {
						inputFile = new File(inputFileUrl.getFile());
						if (inputFile.exists()) {
							log.info("Found file on classpath: {}.", value);
							return inputFile;
						}
					}

				}
			}
			log.error("Failed to resolve file {} for arg[{}]", value, arg.textualName());
			throw new IllegalArgumentException("Invalid or nonexistent file - "+value);
		}
		
	}
}
