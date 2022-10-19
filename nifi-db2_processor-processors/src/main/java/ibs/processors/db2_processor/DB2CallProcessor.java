package ibs.processors.db2_processor;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.dbcp.DBCPService;
import org.jooq.tools.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;


public class DB2CallProcessor extends AbstractProcessor {

	public static final PropertyDescriptor DB2_SERVICE = new PropertyDescriptor.Builder()
			.name("DB2_SERVICE")
			.displayName("service")
			.description("service")
			.required(true)
			.identifiesControllerService(DBCPService.class)
			.build();

	public static final PropertyDescriptor SQL_STRING = new PropertyDescriptor.Builder()
			.name("SQL_STRING")
			.displayName("SQL string")
			.description("SQL string")
			.required(true)
			.defaultValue("")
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
			.build();

	public static final PropertyDescriptor ROWS_NUMBER = new PropertyDescriptor.Builder()
			.name("ROWS_NUMBER")
			.displayName("rows number for select")
			.description("rows number for select, 0 - all rows")
			.required(true)
			.addValidator(StandardValidators.INTEGER_VALIDATOR)
			.defaultValue("0")
			.build();

	public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
			.name("Success")
			.description("The call is successful")
			.build();

	public static final Relationship ERROR_RELATIONSHIP  = new Relationship.Builder()
			.name("Failure")
			.description("Call failed")
			.build();

	private Set<Relationship> relationships;
	private List<PropertyDescriptor> descriptors;

	private int status_code = -1;

	protected DBCPService dbcpService;

	@Override
	protected void init(ProcessorInitializationContext context) {
		super.init(context);

		final ArrayList<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(SQL_STRING);
		descriptors.add(ROWS_NUMBER);
		descriptors.add(DB2_SERVICE);

		this.descriptors = Collections.unmodifiableList(descriptors);

		final Set<Relationship> relationships = new HashSet<Relationship>();

		relationships.add(SUCCESS_RELATIONSHIP);
		relationships.add(ERROR_RELATIONSHIP);

		this.relationships = Collections.unmodifiableSet(relationships);
	}

	@Override
	public Set<Relationship> getRelationships() {
		return this.relationships;
	}

	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

		ComponentLog logger = getLogger();

		FlowFile flowfile = session.create();

		flowfile = session.write(flowfile, out -> {
			try {
				dbcpService = context.getProperty(DB2_SERVICE).asControllerService(DBCPService.class);
				final Connection connection = dbcpService.getConnection();

				StatementHandler stmt_handler = new StatementHandler(
						connection,
						context,
						context.getProperty(SQL_STRING).getValue(),
						Integer.parseInt(context.getProperty(ROWS_NUMBER).getValue()),
						logger
				);

				status_code = stmt_handler.getStatus_code();
				JSONArray res_json = stmt_handler.getJson();

				if (res_json != null){
					out.write(res_json.toString().getBytes(StandardCharsets.UTF_8));
				}
				else {
					logger.debug("No results");
				}
				stmt_handler.close();

			} catch (Exception e) {
				status_code = -1;
				logger.warn(e.getMessage());
				logger.debug(Arrays.toString(e.getStackTrace()));
			}
		}
		);

		switch (status_code){
			case  (-1):
				logger.warn("some error, see log for details");
				session.transfer( flowfile, ERROR_RELATIONSHIP );
				break;

			case (1):
				session.putAttribute(flowfile, "status_code", "1");
				session.transfer( flowfile, SUCCESS_RELATIONSHIP );
				break;

			case (0):
				session.putAttribute(flowfile, "status_code", "0");
				session.transfer( flowfile, SUCCESS_RELATIONSHIP );
				break;
		}
	}
}
