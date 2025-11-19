export const NoteBookFormValues = {
  id: null,
  title: "",
  type: null,
  project: "",
  objective: "",
  protocol: "",
  content: "",
  technicianId: null,
  systemUserId: null,
  status: "",
  sampleIds: [],
  tags: [],
  analyzerIds: [],
  isTemplate: null,
  templateId: null,
  pages: [
    {
      title: "",
      content: "",
      instructions: "",
      tests: [],
    },
  ],
  files: [
    {
      base64File: "",
      fileType: "",
    },
  ],
  comments: [],
};

export const NoteBookInitialData = {
  id: null,
  title: "",
  type: null,
  dateCreated: "",
  status: "NEW",
  tags: [],
  project: "",
  objective: "",
  protocol: "",
  content: "",
  technicianId: null,
  systemUserId: null,
  technicianName: "",
  samples: [],
  analyzers: [],
  pages: [],
  files: [],
  comments: [],
  isTemplate: null,
};

// export const NoteBookInitialData = {
//   id: null,
//   title: "",
//   type: null,
//   lastName: "",
//   firstName: "",
//   gender: "",
//   dateCreated: "",
//   tags: [],
//   project: "",
//   objective: "",
//   protocol: "",
//   content: "",
//   technicianId: null,
//   patientId: null,
//   systemUserId: null,
//   technicianName: "",
//   samples: [
//     {
//       id: null,
//       sampleType: "",
//       collectionDate: "",
//       patientId
//       results: [
//         {
//           test: "",
//           result: "",
//           dateCreated: "",
//         },
//       ],
//     },
//   ],
//   analyserIds: [],
//   pages: [
//     {
//       title: "",
//       content: "",
//       instructions: "",
//     },
//   ],
//   files: [
//     {
//       fileData: "",
//       fileType: "",
//     },
//   ],
// };
