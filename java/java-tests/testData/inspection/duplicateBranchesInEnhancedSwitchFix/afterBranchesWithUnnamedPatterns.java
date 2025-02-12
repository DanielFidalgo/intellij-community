// "Fix all 'Duplicate branches in 'switch'' problems in file" "true"
class C {


  void fooCopy(Object o) {
    switch (o) {
      case Integer _, String _:
        System.out.println(1);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }

  void foo(Object o) {
    switch (o) {
      case Float _:
      case Integer _:
        case String _:
            System.out.println(1);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }

  void foo2(Object o) {
    switch (o) {
      case Integer _ when o.hashCode() == 1:
        case String _:
            System.out.println(1);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }

  void foo3Copy(Object o) {
    switch (o) {
      case Integer _, String _ when o.hashCode() == 1:
        System.out.println(1);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }

  void foo4Copy(Object o) {
    switch (o) {
      case Integer _, String _ when o.hashCode() == 1:
        System.out.println(1);
        break;
      case Number s:
        System.out.println(3);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }

  void foo5(Object o) {
    switch (o) {
      case Integer _ when o.hashCode() == 1:
        case String _ when o.hashCode() == 2:
            System.out.println(1);
        break;
      //can be merged
      case Number s:
        System.out.println(3);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }

  void foo6(Object o) {
    switch (o) {
      case Integer _ when o.hashCode() == 1:
        System.out.println(1);
        break;
      case Number s:
        System.out.println(3);
        break;
      case String _:
        System.out.println(1);
        break;
      default:
        System.out.println(2);
        break;
    }
  }
  void foo7(Object o) {
    switch (o) {
      case Integer _ :
        System.out.println(1);
        break;
      case Number s:
        System.out.println(3);
        break;
      case String _ when o.hashCode() == 1:
        System.out.println(1);
        break;
      default:
        System.out.println(2);
        break;
    }
  }

  void foo8(Object o) {
    switch (o) {
      case Integer _, String _:
        System.out.println(1);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }

  void foo9(Object o) {
    switch (o) {
      case Integer _, CharSequence _ when o.hashCode() == 1:
        System.out.println(1);
        break;
      case Number s:
        System.out.println(3);
        break;
        //can be merged
      default:
        System.out.println(2);
        break;
    }
  }
}