<?php
include('authentication.php');

// Check if the user has the right to add a booking
if ($user['access'] < 3) {
  echo "No message right";
  exit();
}

switch ($_POST['messageAction']) {
  case "messageNotShow":
    $req = $bdd->prepare('UPDATE user 
                          SET message = 0
                          WHERE username = :username;');
    $req->execute(array('username' => $user['username']));
    $req->closeCursor();
    break;
  case "getMessage":
    // Get access level of the user
    $req = $bdd->prepare('SELECT access FROM user WHERE username = :username;');
    $req->execute(array('username' => $user['username']));
    $access = $req->fetch();
    $req->closeCursor();
    // Get the message if it exists
    $req = $bdd->prepare('SELECT message FROM message WHERE id = 1;');
    $req->execute();
    $message = $req->fetch();
    $req->closeCursor();
    // Send result to the user
    $result = array('access' => $access['access']);
    if ($message)
      $result['message'] = $message['message'];
    echo json_encode($result);
    break;
  case "addMessage":
    // Add a new message on the database
    $req = $bdd->prepare('INSERT INTO message (id, message)
                          VALUES (1, :message)
                          ON DUPLICATE KEY UPDATE message = :message;');
    $req->execute(array('message' => $_POST['message']));
    $req->closeCursor();
    // Update message field in users
    $req = $bdd->prepare('UPDATE user SET message = 1;');
    $req->execute();
    $req->closeCursor();
    // Tells the user the action succeeded
    echo "OK";
    break;
  case "removeMessage":
    $req = $bdd->prepare('DELETE FROM message WHERE id = 1;');
    $req->execute();
    $req->closeCursor();
    // Tells the user the action succeeded
    echo "OK";
    break;
}

?>