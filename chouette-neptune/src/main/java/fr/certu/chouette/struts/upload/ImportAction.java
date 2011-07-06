package fr.certu.chouette.struts.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Setter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.util.FileCopyUtils;

import amivif.schema.RespPTLineStructTimetable;
import fr.certu.chouette.echange.ILectureEchange;
import fr.certu.chouette.manager.INeptuneManager;
import fr.certu.chouette.model.neptune.Line;
import fr.certu.chouette.plugin.exchange.ParameterValue;
import fr.certu.chouette.plugin.exchange.SimpleParameterValue;
import fr.certu.chouette.plugin.report.Report;
import fr.certu.chouette.plugin.report.ReportHolder;
import fr.certu.chouette.plugin.report.ReportItem;
import fr.certu.chouette.service.amivif.IAmivifAdapter;
import fr.certu.chouette.service.amivif.ILecteurAmivifXML;
import fr.certu.chouette.service.commun.CodeIncident;
import fr.certu.chouette.service.commun.ServiceException;
import fr.certu.chouette.service.fichier.IImportateur;
import fr.certu.chouette.service.identification.IIdentificationManager;
import fr.certu.chouette.service.importateur.IReducteur;
import fr.certu.chouette.service.importateur.monoitineraire.csv.IImportHorairesManager;
import fr.certu.chouette.service.importateur.monoitineraire.csv.impl.LecteurCSV;
import fr.certu.chouette.service.importateur.monoligne.ILecteurCSV;
import fr.certu.chouette.service.importateur.multilignes.ILecteurPrincipal;
import fr.certu.chouette.service.validation.commun.TypeInvalidite;
import fr.certu.chouette.service.xml.ILecteurEchangeXML;
import fr.certu.chouette.struts.GeneriqueAction;

@SuppressWarnings("serial")
public class ImportAction extends GeneriqueAction {

	private static final Logger log = Logger.getLogger(ImportAction.class);
	private static final String SUCCESS_ITINERAIRE = "success_itineraire";
	private static final String INPUT_ITINERAIRE = "input_itineraire";
	private String fichierContentType;
	private File fichier;
	private boolean incremental;
	private String fichierFileName; 

	@Setter INeptuneManager<Line> lineManager;

	private IImportateur importateur = null;
	private ILecteurCSV lecteurCSV;
	private ILecteurPrincipal lecteurCSVPrincipal;
	private ILecteurPrincipal lecteurCSVHastus;
	private ILecteurPrincipal lecteurCSVPegase;
	private ILecteurPrincipal lecteurXMLAltibus;
	private IIdentificationManager identificationManager;
	private IImportHorairesManager importHorairesManager;
	private ILecteurEchangeXML lecteurEchangeXML;
	private ILecteurAmivifXML lecteurAmivifXML;
	private IAmivifAdapter amivifAdapter;
	private String useAmivif;
	private String useCSVGeneric;
	private String useHastus;
	private String logFileName;
	private IReducteur reducteur;
	private String useAltibus;
	private String usePegase;
	private String importHastusLogFileName;
	private String tmprep;

	private Long idLigne;
	private InputStream inputStream;

	public ImportAction() {
		super();
	}

	public File getImportHastusLogFile() {
		return new File(importHastusLogFileName);
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public InputStream getInputStream() throws Exception {
		return inputStream;
		//return new FileInputStream(logFile.getPath());
	}

	@Override
	public String execute() throws Exception {
		return SUCCESS;
	}

	public String reduireHastus() {
		String canonicalPath = null;
		try {
			canonicalPath = fichier.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String newCanonicalPath = reducteur.reduire(canonicalPath, true);
		addActionMessage(getText("message.reduce.hastus.data") + newCanonicalPath);
		return SUCCESS;
	}

	public String analyseHastus() {
		String canonicalPath = null;
		try {
			canonicalPath = fichier.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String newCanonicalPath = reducteur.reduire(canonicalPath, true);
		try {
			lecteurCSVHastus.lireCheminFichier(newCanonicalPath);
		} catch (ServiceException e) {
			if (CodeIncident.ERR_CSV_NON_TROUVE.equals(e.getCode())) {
				String message = getText("import.csv.fichier.introuvable");
				message += getExceptionMessage(e);
				addActionError(message);
			} else {
				// String defaut = "defaut";
				List<String> args = new ArrayList<String>();
				args.add("");
				args.add("");
				args.add("");
				String message = getText("import.csv.format.ko", args.toArray(new String[0]));
				message += getExceptionMessage(e);
				addActionError(message);
			}
			return INPUT;
		}
		List<ILectureEchange> lecturesEchange = lecteurCSVHastus.getLecturesEchange();
		addActionMessage(getText("message.validate.hastus.data") + logFileName);
		return SUCCESS;
	}

	public String importAltibus() {
		try {
			lecteurXMLAltibus.lireCheminFichier(null);
		} catch (Throwable e) {
			addActionError(getExceptionMessage(e));
			return INPUT;
		}
		addActionMessage(getText("message.import.altibus.success"));
		return SUCCESS;
	}

	public String importPegase() {
		String canonicalPath = null;
		try {
			canonicalPath = fichier.getCanonicalPath();
			log.debug("IMPORT DU FICHIER PEGASE : " + canonicalPath);
		} catch (Exception e) {
			addActionError(getExceptionMessage(e));
			return INPUT;
		}
		try {
			lecteurCSVPegase.lireCheminFichier(canonicalPath);
		} catch (Throwable e) {
			addActionError(getExceptionMessage(e));
			return INPUT;
		}
		boolean echec = false;
		List<ILectureEchange> lecturesEchange = lecteurCSVPegase.getLecturesEchange();
		for (ILectureEchange lectureEchange : lecturesEchange) {
			try {
				importateur.importer(false, lectureEchange, false);
				String[] args = new String[1];
				args[0] = lectureEchange.getLigneRegistration();
				addActionMessage(getText("message.import.pegase.line.success", args));
			} catch (Exception e) {
				echec = true;
				this.addFieldError("fieldName_1", "errorMessage 1");
				this.addFieldError("fieldName_2", "errorMessage 2");

				String[] args = new String[2];
				args[0] = lectureEchange.getLigneRegistration();
				args[1] = getExceptionMessage(e);
				addActionError(getText("message.import.pegase.line.failure", args));
				log.error("Impossible de créer la ligne en base, msg = " + e.getMessage(), e);
			}
		}
		if (echec) {
			return INPUT;
		}
		addActionMessage(getText("message.import.pegase.lines.success"));
		return SUCCESS;
	}

	private void setLog() {
		try {
			inputStream = new FileInputStream(importHastusLogFileName);
		} catch (IOException e) {
		}
	}

	public String importHastusZip() {
		try {
			setLog();
			String result = SUCCESS;
			ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(fichier));
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			if (zipEntry == null) {
				addActionMessage(fichierFileName + " : " + getText("message.import.hastus.bad_zip"));
				//addActionMessage("Rapport : <a href://"+getImportHastusLogFileName()+">"+getImportHastusLogFileName()+"</a>");
				return INPUT;
			}
			while (zipEntry != null) {
				byte[] bytes = new byte[4096];
				int len = zipInputStream.read(bytes);
				File temp = new File(tmprep, zipEntry.getName());
				FileOutputStream fos = new FileOutputStream(temp);
				while (len > 0) {
					fos.write(bytes, 0, len);
					len = zipInputStream.read(bytes);
				}
				if (!result.equals(importHastus(temp))) {
					result = INPUT;
				}
				zipEntry = zipInputStream.getNextEntry();
				temp.delete();
			}
			//addActionMessage("Rapport : <a href://"+getImportHastusLogFileName()+">"+getImportHastusLogFileName()+"</a>");
			return result;
		} catch (Exception e) {
			addActionError("PROBLEM DE LECTURE ARCHIVE : " + getExceptionMessage(e));
			setLog();
			return INPUT;
		}
	}

	public String importHastus(File file) {
		String canonicalPath = null;
		try {
			canonicalPath = file.getCanonicalPath();
		} catch (IOException e) {
			addActionError(getExceptionMessage(e));
			return INPUT;
		}


		if (incremental) {
			log.debug("IMPORT INCREMENTAL DU FICHIER : " + canonicalPath);
		} else {
			log.debug("IMPORT DU FICHIER : " + canonicalPath);
		}

		//String newCanonicalPath = reducteur.reduire(canonicalPath, true);
		//log.debug("DECOMPRESSION VERS LE FICHIER : "+newCanonicalPath);
		try {
			//lecteurCSVHastus.lireCheminFichier(newCanonicalPath);
			lecteurCSVHastus.lireCheminFichier(canonicalPath);
		} catch (Exception e) {
			String message = "";
			//if (CodeIncident.ERR_CSV_NON_TROUVE.equals(e.getCode()))
			//message = getText("import.csv.fichier.introuvable");
			//else
			message = getText("import.csv.format.ko");
			message += " " + getExceptionMessage(e);
			addActionError(message);
			setLog();
			return INPUT;
		}
		boolean echec = false;
		logFileName = lecteurCSVHastus.getLogFileName();
		//logFile = new File(logFileName);
		List<ILectureEchange> lecturesEchange = lecteurCSVHastus.getLecturesEchange();
		for (ILectureEchange lectureEchange : lecturesEchange) {
			try {
				importateur.importer(false, lectureEchange, incremental);
				String[] args = new String[1];
				args[0] = lectureEchange.getLigneRegistration();
				addActionMessage(getText("message.import.hastus.line.success", args));
			} //catch(ServiceException e) {
			catch (Exception e) {
				echec = true;
				this.addFieldError("fieldName_1", "errorMessage 1");
				this.addFieldError("fieldName_2", "errorMessage 2");
				String[] args = new String[2];
				args[0] = lectureEchange.getLigneRegistration();
				args[1] = getExceptionMessage(e);
				addActionError(getText("message.import.hastus.line.failure", args));
				log.error("Impossible de créer la ligne en base, msg = " + e.getMessage(), e);
			}
		}
		if (echec) {
			setLog();
			return INPUT;
		}
		addActionMessage(getText("message.import.hastus.lines.success"));
		setLog();
		return SUCCESS;
	}

	public String importCSVGeneric() 
	{
		String canonicalPath = null;
		try {
			canonicalPath = fichier.getCanonicalPath();
		} catch (Exception e) {
			addActionError(getExceptionMessage(e));
			return INPUT;
		}
		try {
			lecteurCSVPrincipal.lireCheminFichier(canonicalPath);
		} catch (ServiceException e) {
			StackTraceElement[] ste = e.getStackTrace();
			String txt = "";
			if (ste != null) {
				for (int kk = 0; kk < ste.length; kk++) {
					txt += ste[kk].getClassName() + ":" + ste[kk].getLineNumber() + "\n";
				}
			}
			if (CodeIncident.ERR_CSV_NON_TROUVE.equals(e.getCode())) {
				String message = getText("import.csv.fichier.introuvable");
				message += getExceptionMessage(e);
				addActionError(message);
			} else {
				List<String> args = new ArrayList<String>();
				args.add(canonicalPath);
				String message = getText("import.csv.format.multilines.ko", args.toArray(new String[0]));
				message += getExceptionMessage(e);
				addActionError(message);
				//e.printStackTrace();
			}
			return INPUT;
		}
		List<ILectureEchange> lecturesEchange = lecteurCSVPrincipal.getLecturesEchange();
		//Map<String, String> oldTableauxMarcheObjectIdParRef = new HashMap<String, String>();
		//Map<String, String> oldPositionsGeographiquesObjectIdParRef = new HashMap<String, String>();
		//Map<String, String> oldObjectIdParOldObjectId = new HashMap<String, String>();
		for (ILectureEchange lectureEchange : lecturesEchange) {
			try {
				/*
                List<TableauMarche> tableauxMarche = lectureEchange.getTableauxMarche();
                for (TableauMarche tableauMarche : tableauxMarche)
                if (oldTableauxMarcheObjectIdParRef.get(tableauMarche.getComment()) != null)
                tableauMarche.setObjectId(oldTableauxMarcheObjectIdParRef.get(tableauMarche.getComment()));
                lectureEchange.setTableauxMarche(tableauxMarche);
                List<PositionGeographique> positionsGeographiques = lectureEchange.getZonesCommerciales();
                for (PositionGeographique positionGeographique : positionsGeographiques) {
                if (oldPositionsGeographiquesObjectIdParRef.get(positionGeographique.getName()) != null)
                positionGeographique.setObjectId(oldPositionsGeographiquesObjectIdParRef.get(positionGeographique.getName()));
                }
                lectureEchange.setZonesCommerciales(positionsGeographiques);
                List<String> objectIdZonesGeneriques = lectureEchange.getObjectIdZonesGeneriques();
                List<String> tmpObjectIdZonesGeneriques = new ArrayList<String>();
                for (String objectId : objectIdZonesGeneriques)
                if (oldObjectIdParOldObjectId.get(objectId) == null)
                tmpObjectIdZonesGeneriques.add(objectId);
                else
                tmpObjectIdZonesGeneriques.add(oldObjectIdParOldObjectId.get(objectId));
                lectureEchange.setObjectIdZonesGeneriques(tmpObjectIdZonesGeneriques);
                Map<String, String> zoneParenteParObjectId = lectureEchange.getZoneParenteParObjectId();
                Map<String, String> newZoneParenteParObjectId = new HashMap<String,String>();
                for (String objId : zoneParenteParObjectId.keySet()) {
                String commObjectId = zoneParenteParObjectId.get(objId);
                String newCommObjectId = oldObjectIdParOldObjectId.get(commObjectId);
                if (newCommObjectId == null)
                newZoneParenteParObjectId.put(objId, commObjectId);
                else
                newZoneParenteParObjectId.put(objId, newCommObjectId);
                }
                lectureEchange.setZoneParenteParObjectId(newZoneParenteParObjectId);
				 */
				importateur.importer(false, lectureEchange);
				/*
                Map<String, String> _oldTableauxMarcheObjectIdParRef = identificationManager.getDictionaryObjectId().getTableauxMarcheObjectIdParRef();
                for (String key : _oldTableauxMarcheObjectIdParRef.keySet())
                oldTableauxMarcheObjectIdParRef.put(key, _oldTableauxMarcheObjectIdParRef.get(key));

                Map<String, String> _oldPositionsGeographiquesObjectIdParRef = identificationManager.getDictionaryObjectId().getPositionsGeographiquesObjectIdParRef();
                for (String key : _oldPositionsGeographiquesObjectIdParRef.keySet())
                oldPositionsGeographiquesObjectIdParRef.put(key, _oldPositionsGeographiquesObjectIdParRef.get(key));

                Map<String, String> _oldObjectIdParOldObjectId = identificationManager.getDictionaryObjectId().getObjectIdParOldObjectId();
                for (String key : _oldObjectIdParOldObjectId.keySet())
                oldObjectIdParOldObjectId.put(key, _oldObjectIdParOldObjectId.get(key));
				 */
			} catch (Exception e) {
				addActionMessage(getText("message.import.generical.csv.failure"));
				log.error("Impossible de créer la ligne en base, msg = " + e.getMessage(), e);
				return INPUT;
			}
		}
		//identificationManager.getDictionaryObjectId().completion();
		addActionMessage(getText("message.import.generical.csv.success"));
		return SUCCESS;
	}

	public String importCSV() {
		String canonicalPath = null;
		//	Recuperation du chemin du fichier
		try {
			canonicalPath = fichier.getCanonicalPath();
			lecteurCSV.lireCheminFichier(canonicalPath);
		} catch (ServiceException e) {
			if (CodeIncident.ERR_CSV_NON_TROUVE.equals(e.getCode())) {
				String message = getText("import.csv.fichier.introuvable");
				message += getExceptionMessage(e);
				addActionError(message);
			} else {
				// String defaut = "defaut";
				List<String> args = new ArrayList<String>();
				args.add("");
				args.add("");
				args.add("");
				String message = getText("import.csv.format.ko", args.toArray(new String[0]));
				message += getExceptionMessage(e);
				addActionError(message);
			}
			return INPUT;
		} catch (Exception e) {
			addActionError(getExceptionMessage(e));
			return INPUT;
		}
		//	Donnees convertibles en format chouette
		ILectureEchange lectureEchange = lecteurCSV.getLectureEchange();
		//	TODO : Recuperation de l'objet chouettePTNetworkType
		// ChouettePTNetworkType chouettePTNetworkType = exportManager.getExportParRegistration(lectureEchange.getReseau().getRegistrationNumber());
		//	TODO : Validation du fichier CSV
		//	Import des données CSV
		try {
			importateur.importer(true, lectureEchange);
		} catch (ServiceException e) {
			addActionMessage(getText("message.import.csv.failure"));
			log.error("Impossible de creer la ligne en base, msg = " + e.getMessage(), e);
			return INPUT;
		}
		addActionMessage(getText("message.import.csv.success"));
		return SUCCESS;
	}

	/**
	 * Neptune multiple import : zip format but each entry is imported and saved before next one
	 * <br/> to use for large zip files 
	 * 
	 * @return SUCCESS or INPUT
	 * @throws Exception
	 */
	public String importXMLs() throws Exception 
	{
		// migrated in new archietcture
		File temp = null;
		try {
			String result = SUCCESS;
			ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(fichier));
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			while (zipEntry != null) {
				byte[] bytes = new byte[4096];
				int len = zipInputStream.read(bytes);
				temp = new File(tmprep, zipEntry.getName());
				FileOutputStream fos = new FileOutputStream(temp);
				while (len > 0) {
					fos.write(bytes, 0, len);
					len = zipInputStream.read(bytes);
				}
				if (!result.equals(importXML(temp,zipEntry.getName()))) {
					result = INPUT;
				}
				zipEntry = zipInputStream.getNextEntry();
				temp.delete();
			}
			return result;
		} catch (Exception e) {
			addActionError(getExceptionMessage(e) + " : " + temp.getAbsolutePath());
			return INPUT;
		}
	}

	/**
	 * Neptune simple import : xml or zip format but all entries are imported before save step
	 * <br/> to use for small zip files only 
	 * 
	 * @return SUCCESS or INPUT
	 * @throws Exception
	 */
	public String importXML() throws Exception {
		try {
			return importXML(fichier,fichierFileName);
		} catch (Exception e) {
			addActionError(getExceptionMessage(e));
			return INPUT;
		}
	}

	
	/**
	 * import and save a file (xml or zip format)
	 * 
	 * @param file file to import
	 * @param fileName file name for extension identification
	 * @return SUCCESS or INPUT
	 * @throws Exception
	 */
	private String importXML(File file,String fileName) throws Exception 
	{
		try
		{
			String canonicalPath = file.getCanonicalPath();

			if(!FilenameUtils.getExtension(fileName).toLowerCase().equals("xml") && 
					!FilenameUtils.getExtension(fileName).toLowerCase().equals("zip"))
			{
				addActionError(getText("message.import.xml.failure"));
				return INPUT;
			}
			List<ParameterValue> parameters = new ArrayList<ParameterValue>();
			SimpleParameterValue simpleParameterValue = new SimpleParameterValue("xmlFile");
			simpleParameterValue.setFilepathValue(canonicalPath);
			parameters.add(simpleParameterValue);

			//		SimpleParameterValue simpleParameterValue2 = new SimpleParameterValue("validateXML");
			//		simpleParameterValue2.setBooleanValue(validate);
			//		parameters.add(simpleParameterValue2);	

			SimpleParameterValue simpleParameterValue3 = new SimpleParameterValue("fileFormat");
			simpleParameterValue3.setStringValue(FilenameUtils.getExtension(fileName));
			parameters.add(simpleParameterValue3);
			ReportHolder reportHolder = new ReportHolder();

			List<Line> lines = lineManager.doImport(null,"XMLNeptuneLine",parameters, reportHolder);
			
			if(lines != null && !lines.isEmpty())
			{
				if (reportHolder.getReport() != null)
				{
					Report r = reportHolder.getReport();
					log.info(r.getLocalizedMessage());
					logItems("",r.getItems(),Level.INFO);

				}
				for (Line line : lines) 
				{
					List<Line> bid = new ArrayList<Line>();
					bid.add(line);
					lineManager.saveAll(null, bid, true, true);
					String[] args = new String[1];
					args[0] = line.getName();
					addActionMessage(getText("message.import.xml.success", args[0]));
				}
				return SUCCESS;
			}	
			addActionError(getText("message.import.xml.failure"));
			if (reportHolder.getReport() != null)
			{
				Report r = reportHolder.getReport();
				log.error(r.getLocalizedMessage());
				logItems("",r.getItems(),Level.ERROR);

			}
			return INPUT;
		}
		catch (Exception ex)
		{
			manageException(ex);
			return INPUT;
		}
	}

	/**
	 * log report details from import plugins
	 * 
	 * @param indent text indentation for sub levels
	 * @param items report items to log
	 * @param level log level 
	 */
	private void logItems(String indent, List<ReportItem> items, Level level) 
	{
		if (items == null) return;
		for (ReportItem item : items) 
		{
			log.log(level,indent+item.getStatus().name()+" : "+item.getLocalizedMessage());
			logItems(indent+"   ",item.getItems(),level);
		}
		
	}

	public String importAmivifXML() {
		String canonicalPath = copieTemporaire();
		//	Creation de l'objet ChouettePTNetworkType
		RespPTLineStructTimetable amivifLine = null;
		try {
			amivifLine = lecteurAmivifXML.lire(canonicalPath);
		} catch (Exception exception) {
			manageException(exception);
			return INPUT;
		}
		//	Donnees convertibles en format chouette
		ILectureEchange lectureEchange = lecteurEchangeXML.lire(amivifAdapter.getATC(amivifLine));
		//	Import des donnees XML
		try {
			importateur.importer(false, lectureEchange);
		} catch (ServiceException serviceException) {
			addActionError(getText("message.import.amivif.xml.failure"));
			log.error("Impossible de créer la ligne en base, msg = " + serviceException.getMessage(), serviceException);
			return INPUT;
		}
		addActionMessage(getText("message.import.amivif.xml.success"));
		return SUCCESS;
	}

	public String importHorairesItineraire() {
		LecteurCSV lecteurCsvItineraire = new LecteurCSV();
		List<String[]> donneesIn = null;

		try {
			donneesIn = lecteurCsvItineraire.lire(fichier);
		} catch (ServiceException e) {
			if (CodeIncident.ERR_CSV_NON_TROUVE.equals(e.getCode())) {
				String message = getText("import.csv.fichier.introuvable");
				message += getExceptionMessage(e);
				addActionError(message);
			} else {
				// String defaut = "defaut";
				List<String> args = new ArrayList<String>();
				args.add("");
				args.add("");
				args.add("");
				String message = getText("import.csv.format.ko", args.toArray(new String[0]));
				message += getExceptionMessage(e);
				addActionError(message);
			}
			return INPUT_ITINERAIRE;
		} catch (Exception e) {
			e.printStackTrace();
			String message = getText("import.csv.fichier.introuvable");
			message += getExceptionMessage(e);
			addActionError(message);
			return INPUT_ITINERAIRE;
		}


		//	Import des données CSV
		try {
			importHorairesManager.importer(donneesIn);
		} //catch (ServiceException e) 
		catch (Exception e) {
			addActionMessage(getText("message.import.vehicleJourneyAtStop.failure"));
			log.error("Impossible d'importer les horaires de l'itineraire, msg = " + e.getMessage(), e);
			return INPUT_ITINERAIRE;
		}
		addActionMessage(getText("message.import.vehicleJourneyAtStop.success"));
		return SUCCESS_ITINERAIRE;
	}

	/**
	 * process exception
	 * 
	 * @param exception
	 */
	private void manageException(Exception exception) 
	{
		if (exception instanceof ServiceException) 
		{
			if (exception instanceof fr.certu.chouette.service.validation.commun.ValidationException) {
				fr.certu.chouette.service.validation.commun.ValidationException validationException = (fr.certu.chouette.service.validation.commun.ValidationException) exception;
				//	Liste de codes d'erreur 
				List<TypeInvalidite> codeCategories = validationException.getCategories();
				for (TypeInvalidite invalidite : codeCategories) 
				{
					//	Liste des messages d'erreur
					Set<String> messages = validationException.getTridentIds(invalidite);
					int count = 0;
					for (String message : messages) 
					{
						if (count > 5) {
							addActionError("etc...");
							break;
						}
						addActionError(message);
						//log.error(message);
						count++;
					}
				}
			} else {
				ServiceException serviceException = (ServiceException) exception;
				addActionError(getText("message.import.file.exception"));
				log.error("Impossible de recuperer le fichier, msg = " + serviceException.getMessage(), serviceException);
			}
		}
		else
		{
			addActionError(getText("message.import.file.exception"));
			log.error("Impossible de recuperer le fichier, msg = " + exception.getMessage(), exception);
		}

	}

	private String copieTemporaire() {
		try {
			File temp = File.createTempFile("ligne", ".xml");
			FileCopyUtils.copy(fichier, temp);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String canonicalPath = null;
		//	Recupération du chemin du fichier
		try {
			canonicalPath = fichier.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return canonicalPath;
	}

	@Override
	public String input() throws Exception {
		return INPUT;
	}

	public void setImportateur(IImportateur importateur) {
		this.importateur = importateur;
	}

	public void setLecteurCSV(ILecteurCSV lecteurCSV) {
		this.lecteurCSV = lecteurCSV;
	}

	public void setLecteurCSVPrincipal(ILecteurPrincipal lecteurPrincipal) {
		this.lecteurCSVPrincipal = lecteurPrincipal;
	}

	public ILecteurPrincipal getLecteurCSVPrincipal() {
		return lecteurCSVPrincipal;
	}

	public void setLecteurCVSHastus(ILecteurPrincipal lecteurPrincipal) {
		this.lecteurCSVHastus = lecteurPrincipal;
	}

	public ILecteurPrincipal getLecteurCVSHastus() {
		return lecteurCSVHastus;
	}

	public void setLecteurCVSPegase(ILecteurPrincipal lecteurPrincipal) {
		this.lecteurCSVPegase = lecteurPrincipal;
	}

	public ILecteurPrincipal getLecteurCVSPegase() {
		return lecteurCSVPegase;
	}

	public void setLecteurXMLAltibus(ILecteurPrincipal lecteurXMLAltibus) {
		this.lecteurXMLAltibus = lecteurXMLAltibus;
	}

	public String getFichierFileName() {
		return fichierFileName;
	}

	public void setFichierFileName(String fichierFileName) {
		this.fichierFileName = fichierFileName;
	}

	public File getFichier() {
		return fichier;
	}

	public void setFichier(File fichier) {
		this.fichier = fichier;
	}

	public boolean isIncremental() {
		return incremental;
	}

	public void setIncremental(boolean incremental) {
		this.incremental = incremental;
	}

	public String getFichierContentType() {
		return fichierContentType;
	}

	public void setFichierContentType(String fichierContentType) {
		log.debug(fichierContentType);
		this.fichierContentType = fichierContentType;
	}

	public void setLecteurEchangeXML(ILecteurEchangeXML lecteurEchangeXML) {
		this.lecteurEchangeXML = lecteurEchangeXML;
	}

	public void setAmivifAdapter(IAmivifAdapter amivifAdapter) {
		this.amivifAdapter = amivifAdapter;
	}

	public void setLecteurAmivifXML(ILecteurAmivifXML lecteurAmivifXML) {
		this.lecteurAmivifXML = lecteurAmivifXML;
	}

	public String getUseAmivif() {
		return useAmivif;
	}

	public void setUseAmivif(String useAmivif) {
		this.useAmivif = useAmivif;
	}

	public void setUseCSVGeneric(String useCSVGeneric) {
		this.useCSVGeneric = useCSVGeneric;
	}

	public String getUseCSVGeneric() {
		return useCSVGeneric;
	}

	public void setUseHastus(String useHastus) {
		this.useHastus = useHastus;
	}

	public String getUseHastus() {
		return useHastus;
	}

	public void setImportHastusLogFileName(String importHastusLogFileName) {
		this.importHastusLogFileName = importHastusLogFileName;
	}

	public String getImportHastusLogFileName() {
		return importHastusLogFileName;
	}

	public void setUseAltibus(String useAltibus) {
		this.useAltibus = useAltibus;
	}

	public String getUseAltibus() {
		return useAltibus;
	}

	public void setUsePegase(String usePegase) {
		this.usePegase = usePegase;
	}

	public String getUsePegase() {
		return usePegase;
	}

	public void setIdentificationManager(IIdentificationManager identificationManager) {
		this.identificationManager = identificationManager;
	}

	public void setImportHorairesManager(IImportHorairesManager importHorairesManager) {
		this.importHorairesManager = importHorairesManager;
	}

	public IIdentificationManager getIdentificationManager() {
		return identificationManager;
	}

	public Long getIdLigne() {
		return idLigne;
	}

	public void setIdLigne(Long idLigne) {
		this.idLigne = idLigne;
	}

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}

	public void setReducteur(IReducteur reducteur) {
		this.reducteur = reducteur;
	}

	public void setTmprep(String tmprep) {
		this.tmprep = tmprep;
	}

	public String getTmprep() {
		return tmprep;
	}
}