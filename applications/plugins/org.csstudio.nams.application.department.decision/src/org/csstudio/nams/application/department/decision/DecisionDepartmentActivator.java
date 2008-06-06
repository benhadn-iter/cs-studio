/* 
 * Copyright (c) C1 WPS mbH, HAMBURG, GERMANY. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR
 * PURPOSE AND  NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, 
 * REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL
 * PART OF THIS LICENSE. NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER 
 * EXCEPT UNDER THIS DISCLAIMER.
 * C1 WPS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, 
 * ENHANCEMENTS, OR MODIFICATIONS. THE FULL LICENSE SPECIFYING FOR THE 
 * SOFTWARE THE REDISTRIBUTION, MODIFICATION, USAGE AND OTHER RIGHTS AND 
 * OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU 
 * MAY FIND A COPY AT
 * {@link http://www.eclipse.org/org/documents/epl-v10.html}.
 */
package org.csstudio.nams.application.department.decision;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.csstudio.ams.service.preferenceservice.declaration.PreferenceService;
import org.csstudio.ams.service.preferenceservice.declaration.PreferenceServiceJMSKeys;
import org.csstudio.nams.common.material.regelwerk.Regelwerk;
import org.csstudio.nams.common.plugin.utils.BundleActivatorUtils;
import org.csstudio.nams.common.service.ExecutionService;
import org.csstudio.nams.common.service.StepByStepProcessor;
import org.csstudio.nams.service.configurationaccess.localstore.declaration.LocalStoreConfigurationService;
import org.csstudio.nams.service.history.declaration.HistoryService;
import org.csstudio.nams.service.logging.declaration.Logger;
import org.csstudio.nams.service.messaging.declaration.AbstractMultiConsumerMessageHandler;
import org.csstudio.nams.service.messaging.declaration.Consumer;
import org.csstudio.nams.service.messaging.declaration.MessagingService;
import org.csstudio.nams.service.messaging.declaration.MessagingSession;
import org.csstudio.nams.service.messaging.declaration.NAMSMessage;
import org.csstudio.nams.service.messaging.declaration.PostfachArt;
import org.csstudio.nams.service.messaging.declaration.Producer;
import org.csstudio.nams.service.regelwerkbuilder.declaration.RegelwerkBuilderService;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.c1wps.desy.ams.alarmentscheidungsbuero.AlarmEntscheidungsBuero;
import de.c1wps.desy.ams.allgemeines.Ausgangskorb;
import de.c1wps.desy.ams.allgemeines.Eingangskorb;
import de.c1wps.desy.ams.allgemeines.Vorgangsmappe;
import de.c1wps.desy.ams.allgemeines.Vorgangsmappenkennung;

/**
 * <p>
 * The decision department or more precise the activator and application class
 * to controls their life cycle.
 * </p>
 * 
 * <p>
 * <strong>Pay attention:</strong> There are always exactly two instances of
 * this class present: The <emph>bundle activator instance</emph> and the
 * <emph>bundle application instance</emph>. The communication of both is
 * hidden in this class to hide the dirty static singleton communication. This
 * is required during the instantation of extensions (like {@link IApplication})
 * is done in the framework and not by the plug in itself like it should be.
 * Cause of this all service field filled by the <emph>bundles activator</emph>
 * start operation are static to be accessible from the <emph>bundles
 * application</emph> start.
 * </p>
 * 
 * @author <a href="mailto:mz@c1-wps.de">Matthias Zeimer</a>
 * @author <a href="mailto:gs@c1-wps.de">Goesta Steen</a>
 * 
 * @version 0.1-2008-04-25: Created.
 * @version 0.1.1-2008-04-28 (MZ): Change to use {@link BundleActivatorUtils}.
 */
public class DecisionDepartmentActivator implements IApplication,
		BundleActivator {

	/**
	 * The plug-in ID of this bundle.
	 */
	public static final String PLUGIN_ID = "org.csstudio.nams.application.department.decision";

	/**
	 * Gemeinsames Attribut des Activators und der Application: Der Logger.
	 */
	private static Logger logger;

	/**
	 * Gemeinsames Attribut des Activators und der Application: Fatory for
	 * creating Consumers
	 */
	private static MessagingService messagingService;

	/**
	 * Indicates if the application instance should continue working. Unused in
	 * the activator instance.
	 * 
	 * This field is set by another thread to indicate that application should
	 * shut down.
	 */
	private volatile boolean _continueWorking;

	/**
	 * wir nur von der Application benutzt
	 */

	/**
	 * Referenz auf den Thread, welcher die JMS Nachrichten anfragt. Wird
	 * genutzt um den Thread zu "interrupten".
	 */
	private Thread _receiverThread;

	/**
	 * Service für das Entscheidungsbüro um das starten der asynchronen
	 * Ausführung von Einzelaufgaben (Threads) zu kapseln.
	 */
	private static ExecutionService executionService;

	private static PreferenceService preferenceService;

	private static RegelwerkBuilderService regelwerkBuilderService;

	private static HistoryService historyService;

	private MessagingSession amsMessagingSessionForConsumer;

	/**
	 * Consumer zum Lesen auf Alarmnachrichten-Quelle.
	 */
	private Consumer extAlarmConsumer;

	/**
	 * Consumer zum Lesen auf externer-Komando-Quelle.
	 */
	private Consumer extCommandConsumer;

	/**
	 * Consumer zum Lesen auf ams-Komando-Quelle.
	 */
	private Consumer amsCommandConsumer;

	/**
	 * Producer zum Senden auf ams-Zielablage (normally Distributor or
	 * MessageMinder).
	 */
	private Producer amsAusgangsProducer;

	/**
	 * Service to receive configuration-data. Used by
	 * {@link RegelwerkBuilderService}.
	 */
	private static LocalStoreConfigurationService localStoreConfigurationService;

	/**
	 * MessageSession für externe Quellen und Ziele.
	 */
	private MessagingSession extMessagingSessionForConsumer;

	/**
	 * MessageSession für ams interne Quellen und Ziele.
	 */
	private MessagingSession amsMessagingSessionForProducer;

	/**
	 * Starts the bundle activator instance. First Step.
	 * 
	 * @see BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {

		// Services holen...

		// Logging Service
		logger = BundleActivatorUtils
				.getAvailableService(context, Logger.class);

		logger.logInfoMessage(this, "plugin " + PLUGIN_ID
				+ " initializing Services");

		// Messaging Service
		messagingService = BundleActivatorUtils.getAvailableService(context,
				MessagingService.class);
		if (messagingService == null)
			throw new RuntimeException("no messaging service available!");

		// Preference Service (wird als konfiguration verwendet!!)
		preferenceService = BundleActivatorUtils.getAvailableService(context,
				PreferenceService.class);
		if (preferenceService == null)
			throw new RuntimeException("no preference service available!");

		// RegelwerkBuilder Service
		regelwerkBuilderService = BundleActivatorUtils.getAvailableService(
				context, RegelwerkBuilderService.class);
		if (regelwerkBuilderService == null)
			throw new RuntimeException(
					"no regelwerk builder service available!");

		// History Service
		historyService = BundleActivatorUtils.getAvailableService(context,
				HistoryService.class);
		if (historyService == null)
			throw new RuntimeException("no history service available!");

		// LocalStoreConfigurationService
		localStoreConfigurationService = BundleActivatorUtils
				.getAvailableService(context,
						LocalStoreConfigurationService.class);
		if (localStoreConfigurationService == null)
			throw new RuntimeException(
					"no LocalStoreConfigurationService service available!");

		// Execution Service
		// TODO wird noch nicht vollstaendig benutzt! Ins Dec-Office einbauen
		executionService = BundleActivatorUtils.getAvailableService(context,
				ExecutionService.class);
		if (executionService == null)
			throw new RuntimeException("No executor service available!");

		logger.logInfoMessage(this, "plugin " + PLUGIN_ID
				+ " started succesfully.");
	}

	/**
	 * Stops the bundle activator instance. Last Step.
	 * 
	 * @see BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		logger.logInfoMessage(this, "Plugin " + PLUGIN_ID
				+ " stopped succesfully.");
	}

	/**
	 * Starts the bundle application instance. Second Step.
	 * 
	 * @see IApplication#start(IApplicationContext)
	 */
	public Object start(IApplicationContext context) {
		_receiverThread = Thread.currentThread();
		AlarmEntscheidungsBuero alarmEntscheidungsBuero = null;
		StepByStepProcessor ausgangskorbBearbeiter = null;
		_continueWorking = true;

		logger
				.logInfoMessage(this,
						"Decision department application is going to be initialized...");

		try {
			// haben wir durch den preferenceService schon
			// TODO soll diese noch auf gültige Werte geprüft werden
			// logger.logInfoMessage(this,
			// "Decision department application is loading configuration...");

			// Properties properties = initProperties();

			// TODO clientid!! gegebenenfalls aus preferencestore holen
			logger.logInfoMessage(this,
					"Decision department application is creating consumers...");

			amsMessagingSessionForConsumer = messagingService
					.createNewMessagingSession(
							"amsConsumer",
							new String[] {
									preferenceService
											.getString(PreferenceServiceJMSKeys.P_JMS_AMS_PROVIDER_URL_1)
											,
									preferenceService
											.getString(PreferenceServiceJMSKeys.P_JMS_AMS_PROVIDER_URL_2) 
											});
			// TODO clientid!! gegebenenfalls aus preferencestore holen
			extMessagingSessionForConsumer = messagingService
					.createNewMessagingSession(
							"extConsumer",
							new String[] {
									preferenceService
											.getString(PreferenceServiceJMSKeys.P_JMS_EXTERN_PROVIDER_URL_1)
											,
									preferenceService
											.getString(PreferenceServiceJMSKeys.P_JMS_EXTERN_PROVIDER_URL_2) 
											});

			extAlarmConsumer = extMessagingSessionForConsumer
					.createConsumer(
							preferenceService
									.getString(PreferenceServiceJMSKeys.P_JMS_EXT_TOPIC_ALARM),
							PostfachArt.TOPIC);
			extCommandConsumer = extMessagingSessionForConsumer
					.createConsumer(
							preferenceService
									.getString(PreferenceServiceJMSKeys.P_JMS_EXT_TOPIC_COMMAND),
							PostfachArt.TOPIC);
			amsCommandConsumer = amsMessagingSessionForConsumer
					.createConsumer(
							preferenceService
									.getString(PreferenceServiceJMSKeys.P_JMS_AMS_TOPIC_COMMAND),
							PostfachArt.TOPIC);
			logger.logInfoMessage(this,
					"Decision department application is creating producers...");

			// TODO clientid!!
			amsMessagingSessionForProducer = messagingService
					.createNewMessagingSession(
							"amsProducer",
							new String[] { preferenceService
									.getString(PreferenceServiceJMSKeys.P_JMS_AMS_SENDER_PROVIDER_URL) });

			amsAusgangsProducer = amsMessagingSessionForProducer
					.createProducer(
							preferenceService
									.getString(PreferenceServiceJMSKeys.P_JMS_AMS_TOPIC_MESSAGEMINDER),
							PostfachArt.TOPIC);
			/*-
			 * Vor der naechsten Zeile darf niemals ein Zugriff auf die lokale
			 * Cofigurations-DB (application-DB) erfolgen, da zuvor dort noch
			 * keine validen Daten liegen. Der folgende Aufruf blockiert
			 * solange, bis der Distributor bestaetigt, dass die Synchronisation
			 * erfolgreich ausgefuehrt wurde.
			 */
			logger
					.logInfoMessage(
							this,
							"Decision department application orders distributor to synchronize configuration...");
			SyncronisationsAutomat.syncronisationUeberDistributorAusfueren(
					amsAusgangsProducer, amsCommandConsumer,
					localStoreConfigurationService);
			// TODO Hier splitten...
			if (SyncronisationsAutomat.hasBeenCanceled()) {
				// Abbruch bei Syncrinisation
				logger
						.logInfoMessage(
								this,
								"Decision department application was interrupted and requested to shut down during synchroisation of configuration.");
			} else {
				logger
						.logInfoMessage(this,
								"Decision department application is configuring execution service...");
				initialisiereThredGroupTypes(executionService);

				logger
						.logInfoMessage(this,
								"Decision department application is creating decision office...");

				List<Regelwerk> alleRegelwerke = regelwerkBuilderService
						.gibAlleRegelwerke();

				alarmEntscheidungsBuero = new AlarmEntscheidungsBuero(
						alleRegelwerke.toArray(new Regelwerk[alleRegelwerke
								.size()]));
			}
		} catch (Throwable e) {
			logger
					.logFatalMessage(
							this,
							"Exception while initializing the alarm decision department.",
							e);
			_continueWorking = false;
		}

		if (_continueWorking) {
			logger
					.logInfoMessage(this,
							"Decision department application successfully initialized, begining work...");

			// TODO Thread zum auslesen des Ausgangskorbes...

			final Ausgangskorb<Vorgangsmappe> vorgangAusgangskorb = alarmEntscheidungsBuero
					.gibAlarmVorgangAusgangskorb();
			final Eingangskorb<Vorgangsmappe> vorgangEingangskorb = alarmEntscheidungsBuero
					.gibAlarmVorgangEingangskorb();

			// Ausgangskoerbe nebenläufig abfragen
			ausgangskorbBearbeiter = new StepByStepProcessor() {
				@Override
				protected void doRunOneSingleStep() throws Throwable {
					// Vorgangsmappe vorgangZumSenden = vorgangAusgangskorb.???
					// TODO Sende Vorgangsmappe.... (Ausgangskorb erweitern eine
					// entnehme-Operation zu haben).
					// TODO Besser: Decission Office erhält einen "Ausgangskorb"
					// der in Wirklichkeit ein StandardAblagekorb ist
					// der selber auch ein Eingangskorb ist und so kann der
					// DokumentVerbraucher korrekt darauf arbeiten...
					// new DokumentVerbraucherArbeiter<Vorgangsmappe>(new
					// DokumentenBearbeiter<Vorgangsmappe>() {
					//
					// public void bearbeiteVorgang(
					// Vorgangsmappe entnehmeAeltestenEingang)
					// throws InterruptedException {
					// // TODO mache was mit
					//					
					// }
					//				
					// },
					// vorgangAusgangskorb);
				}
			};
			// ausgangskorbBearbeiter.runAsynchronous();

			// start receiving Messages, runs while _continueWorking is true.
			receiveMessagesUntilApplicationQuits(vorgangEingangskorb);
		}
		logger.logInfoMessage(this,
				"Decision department application is shutting down...");

		if (alarmEntscheidungsBuero != null) {
			alarmEntscheidungsBuero
					.beendeArbeitUndSendeSofortAlleOffeneneVorgaenge();
		}

		// Warte auf Thread für Ausgangskorb-Bearbeitung
		if (ausgangskorbBearbeiter != null
				&& ausgangskorbBearbeiter.isCurrentlyRunning()) {
			ausgangskorbBearbeiter.stopWorking();
		}

		// Alle Verbindungen schließen
		amsAusgangsProducer.close();
		amsCommandConsumer.close();
		amsMessagingSessionForConsumer.close();
		amsMessagingSessionForProducer.close();

		extAlarmConsumer.close();
		extCommandConsumer.close();
		extMessagingSessionForConsumer.close();

		logger.logInfoMessage(this,
				"Decision department application successfully shuted down.");
		return IApplication.EXIT_OK;
	}

	private void initialisiereThredGroupTypes(
			ExecutionService executionServiceToBeInitialize) {
		executionServiceToBeInitialize
				.registerGroup(
						ThreadTypesOfDecisionDepartment.ABTEILUNGSLEITER,
						new ThreadGroup(
								ThreadTypesOfDecisionDepartment.ABTEILUNGSLEITER
										.name()));
		executionServiceToBeInitialize
				.registerGroup(
						AbstractMultiConsumerMessageHandler.MultiConsumerMessageThreads.CONSUMER_THREAD,
						new ThreadGroup(
								AbstractMultiConsumerMessageHandler.MultiConsumerMessageThreads.CONSUMER_THREAD
										.name()));
		executionServiceToBeInitialize
				.registerGroup(
						AbstractMultiConsumerMessageHandler.MultiConsumerMessageThreads.HANDLER_THREAD,
						new ThreadGroup(
								AbstractMultiConsumerMessageHandler.MultiConsumerMessageThreads.HANDLER_THREAD
										.name()));
		// TODO Register remaining types!
	}

	/**
	 * This method is receiving Messages and handle them. It will block until
	 * _continueWorking get false.
	 * 
	 * @param eingangskorb
	 *            Der {@link Eingangskorb} to read on.
	 */
	private void receiveMessagesUntilApplicationQuits(
			final Eingangskorb<Vorgangsmappe> eingangskorb) {

		Consumer[] consumerArray = new Consumer[] { amsCommandConsumer,
				extAlarmConsumer, extCommandConsumer };

		AbstractMultiConsumerMessageHandler messageHandler = new AbstractMultiConsumerMessageHandler(
				consumerArray, executionService) {

			public void handleMessage(NAMSMessage message) {
				if (message.enthaeltAlarmnachricht()) {
					try {
						eingangskorb.ablegen(new Vorgangsmappe(
								Vorgangsmappenkennung.createNew(/**
																 * TODO Host
																 * Service statt
																 * new
																 * InetAddress()
																 * .getLocalHost
																 * benutzen
																 */
								InetAddress.getLocalHost(), /**
															 * TODO Calender
															 * Service statt new
															 * Date() benutzen
															 */
								new Date()), message.alsAlarmnachricht()));
					} catch (UnknownHostException e) {
						logger.logFatalMessage(this, "Host unreachable", e);
					} catch (InterruptedException e) {
						logger.logInfoMessage(this,
								"Message receiving interrupted");
					}
				} else if (message.enthaeltSystemnachricht()) {
					if (message.alsSystemachricht()
							.istSyncronisationsAufforderung()) {
						// TODO wir müssen syncronisieren
						// 1. altes office runterfahren und noch offene
						// vorgaenge schicken
						// 2. sychronizieren
						// 3. regel neu laden
						// 4. neues office anlegen
						// 5. neues office straten
					}
				}
				// // TODO andere Nachrichten Typen behandeln
				// // steuer Nachrichten wie z.B.: "Regelwerke neu laden"
				// // oder "einzelne Regelwerke kurzfristig deaktivieren" oder
				// // "shutdown"
			}

		};

		while (_continueWorking) {

			try {
				this._receiverThread.wait();
			} catch (InterruptedException e) {
				// moeglicher interrupt ist ohne auswirkung auf das verhalten
				// des systems
				logger.logDebugMessage(this,
						"wait for receiver thred interrupted", e);
			}
			Thread.yield();
		}

		messageHandler.beendeArbeit();
	}

	/**
	 * Stops the bundle application instance.Ppenultimate Step.
	 * 
	 * @see IApplication#start(IApplicationContext)
	 */
	public void stop() {
		logger.logInfoMessage(this,
				"Shuting down decision department application...");
		_continueWorking = false;
		if (SyncronisationsAutomat.isRunning()) {
			SyncronisationsAutomat.cancel();
		}
		_receiverThread.interrupt();
	}
}
