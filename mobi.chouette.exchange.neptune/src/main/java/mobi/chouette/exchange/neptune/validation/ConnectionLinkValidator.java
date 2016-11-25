package mobi.chouette.exchange.neptune.validation;


import java.util.Map;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.neptune.NeptuneChouetteIdGenerator;
import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.exchange.validation.ValidationException;
import mobi.chouette.exchange.validation.Validator;
import mobi.chouette.exchange.validation.ValidatorFactory;
import mobi.chouette.exchange.validation.report.DataLocation;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import mobi.chouette.model.ChouetteId;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.NeptuneIdentifiedObject;
import mobi.chouette.model.util.Referential;

public class ConnectionLinkValidator extends AbstractValidator implements Validator<ConnectionLink> , Constant{

	public static final String END_OF_LINK = "endOfLink";

	public static final String START_OF_LINK = "startOfLink";

	public static String NAME = "ConnectionLinkValidator";

	private static final String CONNECTION_LINK_1 = "2-NEPTUNE-ConnectionLink-1";

	public static final String LOCAL_CONTEXT = "ConnectionLink";


    @Override
	protected void initializeCheckPoints(Context context)
	{
		addItemToValidation( context, prefix, "ConnectionLink", 1,
				"E");

	}

	public void addLocation(Context context, NeptuneIdentifiedObject object, int lineNumber, int columnNumber)
	{
		addLocation( context,LOCAL_CONTEXT,  object,  lineNumber,  columnNumber);

	}

	public void addStartOfLink(Context context, String objectId, String linkId)
	{
		Context objectContext = getObjectContext(context, LOCAL_CONTEXT, objectId);
		objectContext.put(START_OF_LINK, linkId);

	}

	public void addEndOfLink(Context context, String objectId, String linkId)
	{
		Context objectContext = getObjectContext(context, LOCAL_CONTEXT, objectId);
		objectContext.put(END_OF_LINK, linkId);

	}


	@Override
	public void validate(Context context, ConnectionLink target) throws ValidationException
	{
		Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
		Context localContext = (Context) validationContext.get(LOCAL_CONTEXT);
		Context stopAreaContext = (Context) validationContext.get(StopAreaValidator.LOCAL_CONTEXT);
		NeptuneImportParameters parameters = (NeptuneImportParameters) context.get(CONFIGURATION);
		NeptuneChouetteIdGenerator neptuneChouetteIdGenerator = (NeptuneChouetteIdGenerator) context.get(CHOUETTEID_GENERATOR);
		
		if (localContext == null || localContext.isEmpty()) return ;
		ValidationData data = (ValidationData) context.get(VALIDATION_DATA);
//		Map<String, Location> fileLocations = data.getFileLocations();
		Map<ChouetteId, DataLocation> fileLocations = data.getDataLocations();

		Referential referential = (Referential) context.get(REFERENTIAL);

		// 2-NEPTUNE-ConnectionLink-1 : check presence of start or end of link
		prepareCheckPoint(context, CONNECTION_LINK_1);
		for (String objectId : localContext.keySet()) 
		{
			ConnectionLink connectionLink = referential.getConnectionLinks().get(neptuneChouetteIdGenerator.toChouetteId(objectId, parameters.getDefaultCodespace()));

			if (stopAreaContext.containsKey(connectionLink.getStartOfLink().getChouetteId()) 
					|| stopAreaContext.containsKey(connectionLink.getEndOfLink().getChouetteId()))
				continue;
//			Detail errorItem = new Detail(
//					CONNECTION_LINK_1,
//					fileLocations.get(connectionLink.getChouetteId().getObjectId()));
//			addValidationError(context, CONNECTION_LINK_1, errorItem);
			ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();
			validationReporter.addCheckPointReportError(context, CONNECTION_LINK_1, fileLocations.get(connectionLink.getChouetteId()));

		}
		return ;
	}

	public static class DefaultValidatorFactory extends ValidatorFactory {



		@Override
		protected Validator<ConnectionLink> create(Context context) {
			ConnectionLinkValidator instance = (ConnectionLinkValidator) context.get(NAME);
			if (instance == null) {
				instance = new ConnectionLinkValidator();
				context.put(NAME, instance);
			}
			return instance;
		}

	}

	static {
		ValidatorFactory.factories
		.put(ConnectionLinkValidator.class.getName(), new DefaultValidatorFactory());
	}



}
