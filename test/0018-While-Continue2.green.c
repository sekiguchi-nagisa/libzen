static int f__A(int a0){
   int i1;
   {
      i1 = 0;
      int j2;
      {
         j2 = 0;
         while(i1 < a0){
            i1 = i1 + 1;
            if(i1 == 5) {
               continue continue;
            };
            j2 = j2 + 1;
         };
         return i1 + j2;
      };
   };
}
{
   f__A(10);
}
