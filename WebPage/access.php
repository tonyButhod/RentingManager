<?php
if(!isset($_POST['login']) || !isset($_POST['password'])) {
  echo "Access denied !";
  exit();
}
include('./bdd.php');

$bdd_user = $bdd->prepare('SELECT * FROM user WHERE login = :login AND password = :password');
$bdd_user -> execute(array('login' => $_POST['login'],
                           'password' => hash("sha512", $_POST['password'], false)));
$user = $bdd_user -> fetchAll();
$bdd_user -> closeCursor();

if (isset($user[0])) {
  echo "Welcome ".$user[0]["login"]." ".$user[0]["password"];
}
else {
  echo "Access denied !";
  exit();
}

?>