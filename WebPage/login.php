<?php
include('authentication.php');
  
echo json_encode(array('username' => $user['username'],
                       'hash' => $user['password']));
?>