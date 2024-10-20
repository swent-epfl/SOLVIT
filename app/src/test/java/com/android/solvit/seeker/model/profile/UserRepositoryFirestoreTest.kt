package com.android.solvit.seeker.model.profile

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class UserRepositoryFirestoreTest {

  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockDocumentReference: DocumentReference
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockDocumentSnapshot: DocumentSnapshot
  @Mock private lateinit var mockQuerySnapshot: QuerySnapshot
  @Mock private lateinit var mockTaskUser: Task<UserRepository>
  @Mock private lateinit var mockTaskDoc: Task<DocumentSnapshot>

  private lateinit var firebaseRepository: UserRepositoryFirestore

  private val testSeekerProfile =
      SeekerProfile(
          uid = "12345",
          name = "John Doe",
          username = "johndoe",
          email = "john.doe@example.com",
          phone = "+1234567890",
          address = "Chemin des Triaudes")

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
      FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    firebaseRepository = UserRepositoryFirestore(mockFirestore)

    Mockito.`when`(mockFirestore.collection(any())).thenReturn(mockCollectionReference)
    Mockito.`when`(mockCollectionReference.document(any())).thenReturn(mockDocumentReference)
    Mockito.`when`(mockCollectionReference.document()).thenReturn(mockDocumentReference)
  }

  @Test
  fun getNewUid_returnsDocumentId() {
    Mockito.`when`(mockDocumentReference.id).thenReturn("12345")
    val newUid = firebaseRepository.getNewUid()
    assert(newUid == "12345")
  }

  @Test
  fun getUserProfile_callsFirestoreCollection() {

    // For success

    Mockito.`when`(mockDocumentReference.get()).thenReturn(mockTaskDoc)
    Mockito.`when`(mockTaskDoc.isSuccessful).thenReturn(true)
    Mockito.`when`(mockTaskDoc.result).thenReturn(mockDocumentSnapshot)
    Mockito.`when`(mockDocumentSnapshot.exists()).thenReturn(true)
    `when`(mockTaskDoc.addOnCompleteListener(Mockito.any())).thenAnswer {
      val listener = it.arguments[0] as OnCompleteListener<DocumentSnapshot>
      listener.onComplete(mockTaskDoc)
      mockTaskDoc
    }

    // Mock the document snapshot to return data

    Mockito.`when`(mockDocumentSnapshot.id).thenReturn(testSeekerProfile.uid)
    Mockito.`when`(mockDocumentSnapshot.getString("name")).thenReturn(testSeekerProfile.name)
    Mockito.`when`(mockDocumentSnapshot.getString("username"))
        .thenReturn(testSeekerProfile.username)
    Mockito.`when`(mockDocumentSnapshot.getString("email")).thenReturn(testSeekerProfile.email)
    Mockito.`when`(mockDocumentSnapshot.getString("phone")).thenReturn(testSeekerProfile.phone)
    Mockito.`when`(mockDocumentSnapshot.getString("address")).thenReturn(testSeekerProfile.address)
    val onFailure: () -> Unit = mock()

    firebaseRepository.getUserProfile(
        uid = "12345",
        onSuccess = { profile ->
          assertEquals(testSeekerProfile, profile) // Ensure correct profile is returned
        },
        onFailure = { onFailure() })

    verify(mockDocumentReference).get()
    Mockito.verify(mockTaskDoc).addOnCompleteListener(Mockito.any())
  }

  /*@Test
  fun getUserProfileFail() {
    // Mocking a failed task scenario
    val mockException = Exception("Firestore error")

    // Simulate failure by setting isSuccessful to false and providing an exception
    Mockito.`when`(mockTaskFailure.isSuccessful).thenReturn(false)
    Mockito.`when`(mockTaskFailure.exception).thenReturn(mockException)

    Mockito.`when`(mockDocumentReference.get()).thenReturn(mockTaskFailure)

    // Call the method and verify failure callback is invoked
    firebaseRepository.getUserProfile(
        uid = "12345",
        onSuccess = { TestCase.fail("Success callback should not be called") },
        onFailure = { e ->
          assertEquals(
              mockException, e) // Ensure the failure callback is called with the right exception
        })
  }*/

  @Test
  fun updateUserProfileTest() {
    Mockito.`when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))
    firebaseRepository.updateUserProfile(
        profile = testSeekerProfile, onSuccess = {}, onFailure = {})
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    verify(mockDocumentReference).set(testSeekerProfile)
  }

  @Test
  fun addUserProfileTest() {
    Mockito.`when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))
    firebaseRepository.addUserProfile(profile = testSeekerProfile, onSuccess = {}, onFailure = {})
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    verify(mockDocumentReference).set(testSeekerProfile)
  }

  @Test
  fun getUsersProfile_callsFirestoreCollection() {

    Mockito.`when`(mockCollectionReference.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

    Mockito.`when`(mockQuerySnapshot.documents).thenReturn(listOf())

    firebaseRepository.getUsersProfile(
        onSuccess = {
          // Do nothing; we just want to verify that the 'documents' field was accessed
        },
        onFailure = { TestCase.fail("Failure callback should not be called") })

    verify(timeout(100)) { (mockQuerySnapshot).documents }
  }

  @Test
  fun updateUserProfile_callsFirestoreSet() {
    Mockito.`when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))

    firebaseRepository.updateUserProfile(
        testSeekerProfile,
        onSuccess = { /* Do nothing; success is expected */},
        onFailure = { TestCase.fail("Failure callback should not be called") })

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).set(any())
  }

  @Test
  fun deleteUserProfile_callsFirestoreDelete() {

    Mockito.`when`(mockDocumentReference.delete()).thenReturn(Tasks.forResult(null))

    firebaseRepository.deleteUserProfile(
        "12345",
        onSuccess = { /* Do nothing; success is expected */},
        onFailure = { TestCase.fail("Failure callback should not be called") })

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).delete()
  }

  @Test
  fun documentToUser_success() {
    // Mock a DocumentSnapshot

    // Simulate the document having all the necessary fields
    `when`(mockDocumentSnapshot.id).thenReturn("12345")
    `when`(mockDocumentSnapshot.getString("name")).thenReturn("John Doe")
    `when`(mockDocumentSnapshot.getString("username")).thenReturn("johndoe")
    `when`(mockDocumentSnapshot.getString("email")).thenReturn("john.doe@example.com")
    `when`(mockDocumentSnapshot.getString("phone")).thenReturn("+1234567890")
    `when`(mockDocumentSnapshot.getString("address")).thenReturn("Chemin des Triaudes")

    // Call the helper method
    val profile = firebaseRepository.documentToUser(mockDocumentSnapshot)

    // Assert that the profile was correctly created
    assertEquals("12345", profile?.uid)
    assertEquals("John Doe", profile?.name)
    assertEquals("johndoe", profile?.username)
    assertEquals("john.doe@example.com", profile?.email)
    assertEquals("+1234567890", profile?.phone)
    assertEquals("Chemin des Triaudes", profile?.address)
  }
}
