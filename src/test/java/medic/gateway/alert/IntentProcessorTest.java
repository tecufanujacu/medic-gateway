package medic.gateway.alert;

import android.content.*;
import android.provider.*;
import android.telephony.*;

import java.util.*;

import medic.gateway.alert.test.*;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;

import static android.provider.Telephony.Sms.Intents.*;
import static medic.gateway.alert.WoMessage.Status.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;
import static medic.gateway.alert.test.UnitTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants=BuildConfig.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class IntentProcessorTest {
	private IntentProcessor intentProcessor;

	private DbTestHelper db;
	private Capabilities mockCapabilities;

	@Before
	public void setUp() throws Exception {
		db = new DbTestHelper(RuntimeEnvironment.application);

		intentProcessor = new IntentProcessor();
		mockCapabilities = mockCapabilities(intentProcessor);
	}

	@After
	public void tearDown() throws Exception {
		db.tearDown();
	}

//> INCOMING SMS TESTS
	@Test
	public void test_onReceive_preKitkat_shouldNotIgnore_SMS_RECEIVED_ACTION() {
		// given
		preKitkat();

		// when
		aSmsReceivedActionArrives();

		// then
		db.assertCount("wt_message", 1);
	}

	@Test
	public void test_onReceive_kitkatPlus_shouldIgnore_SMS_RECEIVED_ACTION_ifDefaultSmsApp() {
		// given
		kitkatPlus();
		isDefaultSmsApp();

		// when
		aSmsReceivedActionArrives();

		// then
		db.assertCount("wt_message", 0);
	}

	@Test
	public void test_onReceive_kitkatPlus_shouldNotIgnore_SMS_RECEIVED_ACTION_ifNotDefaultSmsApp() {
		// given
		kitkatPlus();
		isNotDefaultSmsApp();

		// when
		aSmsReceivedActionArrives();

		// then
		db.assertCount("wt_message", 1);
	}

	@Test
	public void test_onReceive_kitkatPlus_shouldNotIgnore_SMS_DELIVERED_ACTION() {
		// given
		kitkatPlus();

		// when
		aSmsDeliveredActionArrives();

		// then
		db.assertCount("wt_message", 1);
	}

	@Test
	public void test_onReceive_shouldStitchConsecutivePartsOfTheSameMessage() {
		// given
		preKitkat();

		// when
		aSmsReceiveActionArrivesWithMultiplePdus(
				A_VALID_MULTIPART_GSM_PDU__PART_1,
				A_VALID_GSM_PDU,
				A_VALID_MULTIPART_GSM_PDU__PART_2,
				A_VALID_GSM_PDU_FROM_THE_MULTIPART_SENDER);

		// then
		db.assertTable("wt_message",
				ANY_ID, "WAITING", ANY_NUMBER, "+447890123456", "Good for you. Slap on the back etc.",
				ANY_ID, "WAITING", ANY_NUMBER, "+447890999999", "Good for you. Slap on the back etc.",
				ANY_ID, "WAITING", ANY_NUMBER, "+447890999999", "Here is a very very very very very very very very very very very very very very very very very very long message which actually spans two other messages apart from the original one!");
	}

//> HELPERS
	private void preKitkat() { when(mockCapabilities.canBeDefaultSmsProvider()).thenReturn(false); }
	private void kitkatPlus() { when(mockCapabilities.canBeDefaultSmsProvider()).thenReturn(true); }

	private void isNotDefaultSmsApp() {
		when(mockCapabilities.isDefaultSmsProvider(RuntimeEnvironment.application))
				.thenReturn(false);
	}
	private void isDefaultSmsApp() {
		when(mockCapabilities.isDefaultSmsProvider(RuntimeEnvironment.application))
				.thenReturn(true);
	}

	private void aSmsDeliveredActionArrives() {
		deliver(smsIntent(SMS_DELIVER_ACTION, A_VALID_GSM_PDU));
	}

	private void aSmsReceivedActionArrives() {
		deliver(smsIntent(SMS_RECEIVED_ACTION, A_VALID_GSM_PDU));
	}

	private void aSmsReceiveActionArrivesWithMultiplePdus(byte[]... pdus) {
		deliver(smsIntent(SMS_RECEIVED_ACTION, pdus));
	}

	private Intent smsIntent(String action, Object... pdus) {
		Intent i = new Intent(action);
		i.putExtra("pdus", pdus);
		i.putExtra("format", "3gpp");
		return i;
	}

	private void deliver(Intent i) {
		intentProcessor.onReceive(RuntimeEnvironment.application, i);
	}
}