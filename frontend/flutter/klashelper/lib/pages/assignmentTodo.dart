import 'package:flutter/material.dart';
import 'package:klashelper/models/workType.dart';
import 'package:klashelper/pages/assignmentFactory.dart';
import 'package:klashelper/models/assignment.dart';
// ignore: must_be_immutable
class AssignmentTodo extends AssignmentFactory {
  AssignmentTodo() : super.create();

  List<Assignment> _assignments =[];
  WorkType _workType;
  
  @override
  void setWorkType(WorkType workType) {
    this._workType = workType;
  }

  @override
  void setAssignments(List<Assignment> assignments) {
    this._assignments = assignments;
  }

  @override
  AssignmentTodoState createState() => AssignmentTodoState();
}

class AssignmentTodoState extends State<AssignmentTodo> with AutomaticKeepAliveClientMixin<AssignmentTodo> {
  @override
  bool get wantKeepAlive => true;
  
  Future<void> _onRefresh() async{
    await Future.delayed(Duration(seconds: 3));
    print("refresh");
  }
  @override
  void initState() {
    print("init todo");
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // TODO: implement build
    return _getList();
  }

  Widget _getList() {
    return //RefreshIndicator(
      //onRefresh: _onRefresh,
      ListView.builder(
        itemCount: widget._assignments.length,
        itemBuilder: (context, index) {
          return Card(
            child: InkWell(
              onTap: () {
                print(index);
                print(this.toString());
                print(widget._workType.toString());
                print(widget._assignments.toString());
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.write("test");
                stringBuffer.write("test");
                print(stringBuffer.toString());
                print(WorkType.HOMEWORK.toString().split('WorkType.')[1]);
                },
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Row(
                  children: <Widget>[
                    _getColumn(index),
                    _getSwitch(index),
                  ],
                ),
              ),
            ),
          );
        },
      );
    //);
  }

  Widget _getColumn(int index) {
    Widget column = Expanded(
      child: Column(
        // align the text to the left instead of centered
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            widget._assignments[index].workCourse,
            style: TextStyle(fontSize: 10),
          ),
          Text(
            widget._assignments[index].workTitle,
            style: TextStyle(fontSize: 16),
          ),
          Text(widget._assignments[index].workFinishTime),
        ],
      ),
    );
    return column;
  }

  Switch _getSwitch(int index) {
    return Switch(
      onChanged: (bool newValue) {
        print(newValue);
        setState(() {
          int isAlarm = (newValue) ? 1 : 0;
          widget._assignments[index].isAlarm = isAlarm;
        });
      },
      value: (widget._assignments[index].isAlarm == 1) ? true : false,
    );
  }
}
