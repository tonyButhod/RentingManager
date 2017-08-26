<?php
include('authentication.php');
  
echo json_encode(array('login' => $user[0]['login'],
                       'hash' => $user[0]['password']));
?>